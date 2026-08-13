# T04图片技术报告

全部正式图片均为最终WebP、1024×1024、质量83；联系表不进入assets或manifest。

| 路径 | 格式 | 尺寸 | 色彩/透明 | bytes | SHA-256 | 人工唯一答案结论 |
|---|---|---|---|---:|---|---|
| `images/characters/char_da_main_v1.webp` | WEBP | 1024×1024 | RGB | 46724 | `ea194964b3626c1dae06694027bbf6d4f7d03a83810f1ebcf01ca05729c4d531` | PASS：主体唯一，无文字/水印；题目图仅一个合理答案 |
| `images/characters/char_kou_main_v1.webp` | WEBP | 1024×1024 | RGB | 45380 | `3fc1b382d38349909f47299cde591ae04e691c7fb4cf6a9c8ce0cef094cb0a76` | PASS：主体唯一，无文字/水印；题目图仅一个合理答案 |
| `images/characters/char_ren_main_v1.webp` | WEBP | 1024×1024 | RGB | 24858 | `d4d334c7fa1589d0f975dfcd171d874817906de10fa45f55e4b048be6487ae1e` | PASS：主体唯一，无文字/水印；题目图仅一个合理答案 |
| `images/characters/char_shan_main_v1.webp` | WEBP | 1024×1024 | RGB | 97298 | `19f1dc6bd7d276740ed7a7359a5f7bfeb0c524da59061821b6b600247da3eed5` | PASS：主体唯一，无文字/水印；题目图仅一个合理答案 |
| `images/characters/char_xiao_main_v1.webp` | WEBP | 1024×1024 | RGB | 44406 | `0fc570199485d08c09fe9ec649fc9eb67187ea65af24df2384f192c9d07b8c3c` | PASS：主体唯一，无文字/水印；题目图仅一个合理答案 |
| `images/options/context_dashan_v1.webp` | WEBP | 1024×1024 | RGB | 65700 | `a35b4f6a430ad7c38beb69eeb51b3ad3747f3d55f126ed51ae1d2ae2b4c0be35` | PASS：主体唯一，无文字/水印；题目图仅一个合理答案 |
| `images/options/context_xiaoshan_v1.webp` | WEBP | 1024×1024 | RGB | 30688 | `ca0b06da3314c8ec147ec310dc3a92ad894931be4b7f35a302651d72edb573ca` | PASS：主体唯一，无文字/水印；题目图仅一个合理答案 |
| `images/options/image_big_watermelon_v1.webp` | WEBP | 1024×1024 | RGB | 31246 | `ee7aea46bc7b68906c2b68014319a09327ee3c87fd49b2f511d6c3e7d054ba6e` | PASS：主体唯一，无文字/水印；题目图仅一个合理答案 |
| `images/options/image_mountain_v1.webp` | WEBP | 1024×1024 | RGB | 98030 | `55507a1117c45f2475fdb588238979b0deee2f901dc52baeb24b055a4decbffb` | PASS：主体唯一，无文字/水印；题目图仅一个合理答案 |
| `images/options/image_mouth_v1.webp` | WEBP | 1024×1024 | RGB | 45320 | `ad2e409943b55442b3f489aa5d67c18ffe71527e5542dcd6f2ccc5b7237773c0` | PASS：主体唯一，无文字/水印；题目图仅一个合理答案 |
| `images/options/image_person_v1.webp` | WEBP | 1024×1024 | RGB | 24190 | `f8c785a5ada97c3d49f6ad32da665128ccba27d174e4d3e9b0cdb7f59fd7ff6c` | PASS：主体唯一，无文字/水印；题目图仅一个合理答案 |
| `images/options/image_small_watermelon_v1.webp` | WEBP | 1024×1024 | RGB | 15504 | `6995a0fc61c9a01a0a0e6f10fd3f795c173197a0d1c103d5090abae21e81988c` | PASS：主体唯一，无文字/水印；题目图仅一个合理答案 |

## 大山/小山返工复核

两图由同一RGBA山体母版等比缩放得到，使用同一像素级1024×1024背景、相同中心线与底部基线；只改变山体尺寸，不使用云层、颜色、位置、文字或边框提示答案。
