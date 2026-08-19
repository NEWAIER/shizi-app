"""Fill the next DRAFT characters for child-pack-v2-300.

This only creates reviewable CSV source rows. It deliberately does not create
media files or activate an Android pack.
"""
from __future__ import annotations

import csv
from pathlib import Path

from pypinyin import Style, pinyin


ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "content-source" / "child-pack-v2-300"
EXISTING = set("人口大小手头爸妈家门山水火木日月天空云雨风牛羊鸟鱼马猫狗兔虫花衣食米饭杯书车灯床桌看听走跑来去上下开关")
CHARACTER_POOL = "我你他她它爱友同学校老师生字文画读写认知想要给和在这那哪谁什么一二三四五六七八九十百千个只本张件条些每都也还又再很最真太今明年时分秒周岁点次边方向内里外高低长短多少女快慢早晚地土石田草树果春夏秋冬白红黄绿黑星光新老好有无是不会见说话喝美笑颜色形状圆方轻重冷热香甜苦酸软硬干湿声音歌舞乐球棒绳车船飞机路桥门窗房屋桌椅床被衣帽鞋袜包伞灯钟刀勺碗盘筷茶奶菜肉蛋猪鸡鸭鹅熊虎狮象猴鹿鼠牙耳眼鼻脸脚心身力气毛皮骨穿站坐笑哭住玩做用找拿放故事朋友哥哥姐姐弟妹宝宝孩子先生小姐医生护士警察农民工人谢谢请问对不起欢迎快乐安全帮助喜欢游戏生日礼物春节中秋太阳月亮彩虹森林草原公园学校医院商店厨房厕所房间院子城市乡村中国世界河海湖江岛店馆楼台顶角线面豆瓜盐糖油汤粥饼桃梨蕉芽苗叶枝根皮豆芽蛋糕米粉肉丸盘碗筷勺衣裤裙帽巾袜鞋镜伞锁钥匙盒袋瓶罐旗灯影声香味甜苦热冷温暖清亮圆满开心勇敢聪明平安一起自己大家我们他们里面外面这里那里多少几个每个已经正在马上以后以前今天昨天明天去年今年时间" 


def meaning(char: str) -> str:
    return f"“{char}”是生活中常见的字宝宝，我们在故事和游戏里会遇到它。"


def main() -> None:
    SOURCE.mkdir(parents=True, exist_ok=True)
    characters_path = SOURCE / "characters.csv"
    questions_path = SOURCE / "questions.csv"
    character_fields = ["id", "character", "pinyin", "tone", "meaning_for_child", "word_1", "word_2", "sentence", "image_prompt", "audio_required", "review_status"]
    question_fields = ["question_id", "character_id", "question_type", "correct_character", "option_character_1", "option_character_2", "option_character_3", "prompt_audio", "review_status"]
    with questions_path.open(encoding="utf-8", newline="") as file:
        questions = list(csv.DictReader(file))
    if characters_path.exists() and characters_path.stat().st_size:
        with characters_path.open(encoding="utf-8", newline="") as file:
            characters = list(csv.DictReader(file))
    else:
        # Recover a draft batch if a previous generation was interrupted after
        # questions.csv was written but before characters.csv was finalized.
        characters = []
        seen = set()
        for question in questions:
            char = question["correct_character"]
            if char in seen:
                continue
            seen.add(char)
            characters.append({
                "id": question["character_id"],
                "character": char,
                "pinyin": pinyin(char, style=Style.TONE, heteronym=False)[0][0],
                "tone": "0",
                "meaning_for_child": meaning(char),
                "word_1": f"{char}人",
                "word_2": f"{char}家",
                "sentence": f"我在故事里看见“{char}”。",
                "image_prompt": f"儿童绘本风：用清楚温暖的生活场景表现“{char}”的意思，无文字",
                "audio_required": "character meaning words sentence",
                "review_status": "DRAFT",
            })
    existing_chars = {row["character"] for row in characters} | EXISTING
    additions = []
    if len(characters) < 300:
        for char in CHARACTER_POOL:
            if char in existing_chars or char in {row["character"] for row in additions}:
                continue
            tone_pinyin = pinyin(char, style=Style.TONE, heteronym=False)[0][0]
            additions.append({
                "id": f"char_u{ord(char):04x}",
                "character": char,
                "pinyin": tone_pinyin,
                "tone": "0",
                "meaning_for_child": meaning(char),
                "word_1": f"{char}人",
                "word_2": f"{char}家",
                "sentence": f"我在故事里看见“{char}”。",
                "image_prompt": f"儿童绘本风：用清楚温暖的生活场景表现“{char}”的意思，无文字",
                "audio_required": "character meaning words sentence",
                "review_status": "DRAFT",
            })
            if len(characters) + len(additions) >= 300:
                break
    if len(characters) + len(additions) != 300:
        raise SystemExit(f"需要补齐到300字，当前可用新增字数不足：{len(characters) + len(additions)}")

    with characters_path.open("w", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=character_fields)
        writer.writeheader()
        writer.writerows(characters + additions)

    all_chars = [row["character"] for row in characters + additions]
    for index, row in enumerate(additions, start=len(questions)):
        distractors = [all_chars[(index + offset) % len(all_chars)] for offset in (1, 2, 3)]
        questions.append({
            "question_id": f"q_{row['id']}_listen",
            "character_id": row["id"],
            "question_type": "LISTEN_CHOOSE_CHARACTER",
            "correct_character": row["character"],
            "option_character_1": distractors[0],
            "option_character_2": distractors[1],
            "option_character_3": distractors[2],
            "prompt_audio": "draft",
            "review_status": "DRAFT",
        })
    with questions_path.open("w", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=question_fields)
        writer.writeheader()
        writer.writerows(questions)
    media_rows = []
    review_rows = []
    for row in characters + additions:
        cid = row["id"]
        for asset_type, asset_path in (
            ("image", f"images/characters/{cid}_main_v1.webp"),
            ("character_audio", f"audio/characters/{cid}_v1.mp3"),
            ("meaning_audio", f"audio/meanings/meaning_{cid}_v1.mp3"),
            ("word_audio_1", f"audio/words/{cid}_1_v1.mp3"),
            ("word_audio_2", f"audio/words/{cid}_2_v1.mp3"),
            ("sentence_audio", f"audio/sentences/{cid}_v1.mp3"),
        ):
            media_rows.append({"character_id": cid, "asset_type": asset_type, "asset_path": asset_path, "required": "true", "review_status": "DRAFT"})
        review_rows.append({"character_id": cid, "text_status": "DRAFT", "image_status": "DRAFT", "audio_status": "DRAFT", "parent_status": "DRAFT", "reviewer": "", "notes": "等待儿童内容、媒体和家长审核"})
    with (SOURCE / "media.csv").open("w", encoding="utf-8", newline="") as file:
        media_fields = ["character_id", "asset_type", "asset_path", "required", "review_status"]
        writer = csv.DictWriter(file, fieldnames=media_fields)
        writer.writeheader()
        writer.writerows(media_rows)
    with (SOURCE / "reviews.csv").open("w", encoding="utf-8", newline="") as file:
        review_fields = ["character_id", "text_status", "image_status", "audio_status", "parent_status", "reviewer", "notes"]
        writer = csv.DictWriter(file, fieldnames=review_fields)
        writer.writeheader()
        writer.writerows(review_rows)
    print(f"child-pack-v2-300 source ready: {len(characters + additions)} characters, {len(questions)} questions")


if __name__ == "__main__":
    main()
