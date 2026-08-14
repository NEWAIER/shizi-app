# 基础汉字来源追踪 V1

批量基础字在进入儿童加工层前，必须有独立的 `source_provenance.json`。它记录来源标识、许可证、快照标识、快照 SHA-256 和审核状态。

检查命令：

```powershell
python tools/content-builder/validate_foundation_provenance.py `
  --records content-source/content-strategy-v1/batch/character_base.json `
  --provenance content-source/content-strategy-v1/batch/source_provenance.json `
  --allow-draft
```

当前示例的来源状态是 `DRAFT`，并使用全零摘要作为占位，因此只能通过联调门禁，不能作为发布依据。接入真实来源后，必须替换为真实许可证、快照 ID 和 SHA-256，并将状态改为 `VERIFIED`；不允许伪造来源信息。
