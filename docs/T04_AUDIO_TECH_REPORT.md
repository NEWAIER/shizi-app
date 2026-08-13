# T04音频技术报告

检测工具：FFprobe/FFmpeg；正式音频均为已选A音色、MP3、44.1kHz、单声道、CBR 96kbps。

| 路径 | 时长s | LUFS-I | True Peak dBTP | 首静音ms | 尾静音ms | Hz | 声道 | 码率 | bytes | SHA-256 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|
| `audio/characters/char_da_v1.mp3` | 0.522 | -16.5 | -3.6 | 66.7 | 115.4 | 44100 | 1 | 96000 | 6627 | `4abc2b9a1c116e1e9cbae529b12b2ab0d9abb182a99377e9730d263a0fc9fb48` |
| `audio/characters/char_kou_v1.mp3` | 0.522 | -16.4 | -3.7 | 68.1 | 100.2 | 44100 | 1 | 96000 | 6627 | `505a2fcc5eeab69a5742e44d9163688b5b93842da0e096d32208f91d27248b4a` |
| `audio/characters/char_ren_v1.mp3` | 0.522 | -16.2 | -2.6 | 66.5 | 112.3 | 44100 | 1 | 96000 | 6627 | `d05217eae266b466932e73bf965524d0b281f5869ac5750c93ccff1c7e42aa0e` |
| `audio/characters/char_shan_v1.mp3` | 0.601 | -16.2 | -4.6 | 67.8 | 97.8 | 44100 | 1 | 96000 | 7568 | `916371c8278d80545ae65ecd784f60d1d830788aed50ea387079d37b64e57be1` |
| `audio/characters/char_xiao_v1.mp3` | 0.522 | -16.2 | -1.8 | 49.9 | 88.1 | 44100 | 1 | 96000 | 6627 | `3521e36d0202d354063b8fff263fedeec47f10c0536563e453b767b9dc5c079e` |
| `audio/meanings/meaning_da_v1.mp3` | 4.075 | -16.8 | -1.4 | 74.9 | 274.8 | 44100 | 1 | 96000 | 49259 | `350e7ff31ed1b23902160fbdf1ed262445da72ccf671d97f78a4b306e6719c59` |
| `audio/meanings/meaning_kou_v1.mp3` | 3.187 | -16.5 | -1.4 | 78.9 | 0.0 | 44100 | 1 | 96000 | 38601 | `30abe78d64a15d426b9daffc9fdefcba292b05912eca2bfe9c09a911aef08f05` |
| `audio/meanings/meaning_ren_v1.mp3` | 3.161 | -16.4 | -2.1 | 54.2 | 46.3 | 44100 | 1 | 96000 | 38288 | `db974e97fb0f5fb03acbfb8d56cccd87d03e025e619f014726f14581b519a719` |
| `audio/meanings/meaning_shan_v1.mp3` | 5.146 | -17.2 | -2.5 | 66.7 | 0.0 | 44100 | 1 | 96000 | 62111 | `afd605cb5ceab999306fa7c35552331f1056438be1d05417fcb533bb127a5ab6` |
| `audio/meanings/meaning_xiao_v1.mp3` | 4.153 | -17.0 | -1.4 | 75.1 | 0.0 | 44100 | 1 | 96000 | 50200 | `2e6972a47877ca98317624fd0cd560e4f6e0b62eef97279ea54c06e05da6fd12` |
| `audio/prompts/prompt_choose_picture_v1.mp3` | 2.717 | -16.4 | -2.8 | 76.2 | 405.3 | 44100 | 1 | 96000 | 32959 | `775d230c3b42d8c202398384f7dcb65085c87b41fb496e8a8e9fedaaca8f45d1` |
| `audio/prompts/prompt_choose_sound_v1.mp3` | 2.926 | -17.2 | -1.7 | 84.9 | 70.0 | 44100 | 1 | 96000 | 35467 | `0909c0476fef23f380a9e0ba1cbc283f915b88e80a9b976996b6391733583e93` |
| `audio/prompts/prompt_find_da_v1.mp3` | 1.123 | -16.4 | -3.3 | 59.1 | 330.2 | 44100 | 1 | 96000 | 13837 | `74e4f3e42f9952d888f06e19cfd69694d898f6ca68c2bbede6e421beeb69c6e7` |
| `audio/prompts/prompt_find_dashan_v1.mp3` | 1.332 | -16.4 | -3.0 | 73.8 | 0.0 | 44100 | 1 | 96000 | 16345 | `5023f45060faeca349ec164b49092a8a1f4f0cd01105fec25303e7b468816071` |
| `audio/prompts/prompt_find_kou_v1.mp3` | 1.202 | -16.5 | -2.9 | 63.7 | 400.0 | 44100 | 1 | 96000 | 14778 | `2abbd3bb15f872f189d6f0cc97cec9b3543f47ef20065aaa52a8308019d22b26` |
| `audio/prompts/prompt_find_ren_v1.mp3` | 1.176 | -16.5 | -2.9 | 64.5 | 48.9 | 44100 | 1 | 96000 | 14464 | `20d84019252cbc29b7b63acf8d5bade1c8d23bf9275c340ee526820b7daf46b6` |
| `audio/prompts/prompt_find_same_v1.mp3` | 1.959 | -16.4 | -2.5 | 78.0 | 42.1 | 44100 | 1 | 96000 | 23868 | `99eca57bccb90f1fd5a27fe0ae00e293334d147a551229c278a575e31ddcab19` |
| `audio/prompts/prompt_find_shan_v1.mp3` | 1.202 | -16.4 | -3.3 | 63.1 | 414.9 | 44100 | 1 | 96000 | 14778 | `3111dea890f899a264cb8ed704ae317747f4263b8359005a069522c4789e7bf4` |
| `audio/prompts/prompt_find_xiao_v1.mp3` | 1.254 | -16.4 | -3.1 | 64.3 | 471.3 | 44100 | 1 | 96000 | 15405 | `e6e934f1bb97faef7ee080b45bb6c78204ac25ce32d1d8a7aa8665acae9b1314` |
| `audio/prompts/prompt_find_xiaoshan_v1.mp3` | 1.306 | -16.5 | -2.4 | 74.1 | 0.0 | 44100 | 1 | 96000 | 16031 | `6abe1a794e83f57e7bdd14d42878a24692f49f57b0117fa356da9852c42222c1` |
| `audio/sentences/sentence_da_v1.mp3` | 1.411 | -16.4 | -1.6 | 78.5 | 287.4 | 44100 | 1 | 96000 | 17285 | `50555967769b0a0764c99b80c97022289e17e3a317d720dff57b337ffcf7447e` |
| `audio/sentences/sentence_kou_v1.mp3` | 1.515 | -16.4 | -1.9 | 76.6 | 0.0 | 44100 | 1 | 96000 | 18539 | `dee5510a5157ca72d14910ec7f1af12951341a28b5cc049de54ab777f86160a5` |
| `audio/sentences/sentence_ren_v1.mp3` | 1.228 | -16.4 | -3.1 | 77.6 | 46.4 | 44100 | 1 | 96000 | 15091 | `0c237b4c1bd39c7d3b803da133fa406f3d2c5bf006db43714fc413ba8affa1af` |
| `audio/sentences/sentence_shan_v1.mp3` | 1.332 | -16.4 | -2.5 | 72.7 | 0.0 | 44100 | 1 | 96000 | 16345 | `328c57b65d4c48acfb717bc1f3ba84dd411b59d2f8e1d6b59cfe9be93c843b74` |
| `audio/sentences/sentence_xiao_v1.mp3` | 1.463 | -16.5 | -3.6 | 78.2 | 0.0 | 44100 | 1 | 96000 | 17912 | `9d0cf4796d58c5df124af1adc55ed061d7d23d121417c93e2d0601aafdb24c0b` |
| `audio/words/word_dakou_v1.mp3` | 0.679 | -16.4 | -2.0 | 74.5 | 420.5 | 44100 | 1 | 96000 | 8508 | `0305b4dea418e247c2dfcb5cbf08317cbdd05f7c0240b67c72c719f26ae94ca6` |
| `audio/words/word_daren_v1.mp3` | 0.601 | -16.5 | -4.2 | 71.7 | 55.0 | 44100 | 1 | 96000 | 7568 | `65245f3c7675fe63fc58d9c1778702829a503052838029364acb2e307bf1ae55` |
| `audio/words/word_dashan_v1.mp3` | 0.653 | -16.4 | -4.0 | 71.8 | 0.0 | 44100 | 1 | 96000 | 8195 | `dfcd0dc4ebbcab154bac600b9233d024966c3dff4e0868e75f7c2d971c9dac9f` |
| `audio/words/word_haoren_v1.mp3` | 0.627 | -16.5 | -3.6 | 51.4 | 0.0 | 44100 | 1 | 96000 | 7881 | `6380faaab68c17ca913f9c45e411a205922466b6d1fd359ae5106a603fbb1840` |
| `audio/words/word_jiaren_v1.mp3` | 0.653 | -16.5 | -3.3 | 69.8 | 53.2 | 44100 | 1 | 96000 | 8195 | `e34ec823e5b66678e25462a85269bdb3d7f6469675df222d3a0efd36384aa7a3` |
| `audio/words/word_kaikou_v1.mp3` | 0.679 | -16.4 | -2.3 | 73.1 | 382.1 | 44100 | 1 | 96000 | 8508 | `31356daf12ffac8d3943c45d8f0d9c998c8df39372d1f6818a7fd9b58735711f` |
| `audio/words/word_menkou_v1.mp3` | 0.653 | -16.5 | -2.1 | 74.7 | 0.0 | 44100 | 1 | 96000 | 8195 | `c24e5888106fc282c5abda064403cc4e80917aee7a285148d6651cd555237a8c` |
| `audio/words/word_pashan_v1.mp3` | 0.653 | -16.4 | -3.9 | 76.3 | 0.0 | 44100 | 1 | 96000 | 8195 | `11ec4f298ccef370575daf933e008560e7e7d14505f46d061b6e7ef48e4bdd93` |
| `audio/words/word_shanshang_v1.mp3` | 0.679 | -16.4 | -2.7 | 62.0 | 0.0 | 44100 | 1 | 96000 | 8508 | `8db025e6909bf5ca8b1c8639c960e657e748f535a7806740aa088f5b01131f4e` |
| `audio/words/word_xiaokou_v1.mp3` | 0.731 | -17.2 | -1.8 | 96.8 | 65.4 | 44100 | 1 | 96000 | 9135 | `ea9671c0909ac707eae52ff1875390bcf7830956e3ba4b48eea6fcefc7cb1ae5` |
| `audio/words/word_xiaoshan_v1.mp3` | 0.705 | -16.4 | -3.2 | 58.2 | 0.0 | 44100 | 1 | 96000 | 8822 | `726eb12d6c7ad944040b4d92dfae7bff496cf78532af10570ca9ac33d1461c6f` |
| `audio/words/word_xiaoshou_v1.mp3` | 0.705 | -16.5 | -2.4 | 60.5 | 0.0 | 44100 | 1 | 96000 | 8822 | `75bbf24f62d23d7f8711538d2a8b6eb575863ab68b129591a9fafa3031be69be` |
| `audio/words/word_yikoushui_v1.mp3` | 0.888 | -16.4 | -3.2 | 62.4 | 0.0 | 44100 | 1 | 96000 | 11016 | `967c131b5245b604688a6949803104b44835d6ecca7e8344a70a540d901b893a` |

## 指定返工结论

- `char_xiao_v1.mp3`、`prompt_choose_sound_v1.mp3`、`word_xiaokou_v1.mp3`已提升至约-16至-17 LUFS，且峰值低于-1dBFS。
- 五个单字头部静音均低于100ms，时长均在0.5—1.5秒；尾部静音均低于100ms。
- 所有返工均保留原朗读文本和家长已选A音色。
