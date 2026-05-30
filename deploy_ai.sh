#!/bin/bash
# ============================================================
#  WYY AI Service v2 一键部署脚本
#  用法 (在 Kali 上执行):
#    bash /mnt/hgfs/aaopenclow-share/music_wyy/deploy_ai.sh
# ============================================================
set -e

SRC="/mnt/hgfs/aaopenclow-share/music_wyy/ai/ai_service/main.py"
DST="/opt/wyy_ai/ai_service/main.py"

echo "=== WYY AI Service v2 部署 ==="
echo ""

# 1. 验证源文件
if [ ! -f "$SRC" ]; then
    echo "❌ 源文件不存在: $SRC"
    echo "   请确保共享文件夹已挂载"
    exit 1
fi
echo "✅ 源文件: $SRC"

# 2. 备份旧文件
if [ -f "$DST" ]; then
    cp "$DST" "${DST}.bak.v1"
    echo "✅ 已备份旧版本 → ${DST}.bak.v1"
fi

# 3. 复制新文件
cp "$SRC" "$DST"
echo "✅ 已复制 v2 主程序 → $DST"

# 4. 检查依赖 (httpx)
echo ""
echo "--- 检查 Python 依赖 ---"
if /opt/wyy_ai/venv/bin/pip show httpx > /dev/null 2>&1; then
    echo "✅ httpx 已安装"
else
    echo "⚠️  httpx 未安装，正在安装..."
    /opt/wyy_ai/venv/bin/pip install httpx
    echo "✅ httpx 安装完成"
fi

# 5. 清除旧缓存 (v1 的会无效，v2 有版本前缀)
echo ""
echo "--- 清除旧缓存 ---"
curl -s -X DELETE http://127.0.0.1:8100/api/cache -H "Content-Type: application/json" -d '{}' || echo "(服务尚未启动，启动后手动清除)"

# 6. 重启服务
echo ""
echo "--- 重启服务 ---"
systemctl restart wyy-ai
sleep 3

# 7. 验证
echo ""
echo "--- 验证 ---"
HEALTH=$(curl -s http://127.0.0.1:8100/health)
echo "$HEALTH" | python3 -m json.tool 2>/dev/null || echo "$HEALTH"

if echo "$HEALTH" | grep -q '"status":"ok"'; then
    echo ""
    echo "========================================="
    echo "  ✅ 部署成功! AI Service v2 已就绪"
    echo "  Version: $(echo $HEALTH | grep -o '"version":"[^"]*"')"
    echo "========================================="
else
    echo ""
    echo "❌ 服务健康检查失败，请查看日志:"
    echo "   journalctl -u wyy-ai -f"
    exit 1
fi
