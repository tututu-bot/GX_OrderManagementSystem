package com.example.gx_ordersystem.controller;

import com.example.gx_ordersystem.common.Result;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模拟支付控制器
 * 实现完整的扫码支付模拟流程：
 * 1. 前端创建支付会话 → 返回 token
 * 2. 生成二维码（URL 指向支付确认页）
 * 3. 手机扫码打开支付页 → 点击确认
 * 4. 前端轮询支付状态 → 确认后自动提交订单
 */
@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Value("${server.port}")
    private int serverPort;

    private static final ConcurrentHashMap<String, PaymentSession> sessions = new ConcurrentHashMap<>();

    /**
     * 获取服务器局域网 IP，通过 UDP 探测确定手机可访问的地址
     */
    private static String getLanIp() {
        // 方式1：UDP 探测（最可靠，找到对外通信的网卡 IP）
        try (java.net.DatagramSocket socket = new java.net.DatagramSocket()) {
            socket.connect(InetAddress.getByName("114.114.114.114"), 10002);
            String ip = socket.getLocalAddress().getHostAddress();
            if (ip != null && !ip.startsWith("127.")) {
                return ip;
            }
        } catch (Exception ignored) {}

        // 方式2：遍历网卡（过滤虚拟网卡）
        try {
            String[] skipNames = {"docker", "veth", "vmnet", "virbr", "vboxnet", "utun", "llw", "awdl", "bridge"};
            String[] preferNames = {"en0", "eth0", "wlan0", "eth1", "en1"};
            String fallback = null;

            for (NetworkInterface iface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!iface.isUp() || iface.isLoopback()) continue;
                String name = iface.getName().toLowerCase();
                boolean skip = false;
                for (String s : skipNames) { if (name.contains(s)) { skip = true; break; } }
                if (skip) continue;

                for (InetAddress addr : Collections.list(iface.getInetAddresses())) {
                    if (addr.isLoopbackAddress() || addr.isLinkLocalAddress()) continue;
                    if (!(addr instanceof java.net.Inet4Address)) continue;
                    String ip = addr.getHostAddress();
                    if (ip.startsWith("127.")) continue;

                    for (String p : preferNames) {
                        if (name.equals(p)) return ip;
                    }
                    if (fallback == null) fallback = ip;
                }
            }
            if (fallback != null) return fallback;
        } catch (Exception ignored) {}
        return "127.0.0.1";
    }

    static class PaymentSession {
        String paymentMethod;
        BigDecimal amount;
        String status;
        LocalDateTime createTime;

        PaymentSession(String paymentMethod, BigDecimal amount) {
            this.paymentMethod = paymentMethod;
            this.amount = amount;
            this.status = "PENDING";
            this.createTime = LocalDateTime.now();
        }
    }

    /**
     * 创建支付会话，返回 token 供前端生成二维码
     */
    @PostMapping("/create")
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        String paymentMethod = body.get("paymentMethod").toString();
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        PaymentSession session = new PaymentSession(paymentMethod, amount);
        sessions.put(token, session);

        // 清理超过 10 分钟的过期会话
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(10);
        sessions.entrySet().removeIf(e -> e.getValue().createTime.isBefore(cutoff));

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("amount", amount);
        result.put("paymentMethod", paymentMethod);
        return Result.success(result);
    }

    /**
     * 轮询支付状态（前端调用，需要登录）
     */
    @GetMapping("/status/{token}")
    public Result<Map<String, Object>> status(@PathVariable String token) {
        PaymentSession session = sessions.get(token);
        if (session == null) {
            return Result.error("支付会话不存在或已过期");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("status", session.status);
        result.put("paymentMethod", session.paymentMethod);
        result.put("amount", session.amount);
        return Result.success(result);
    }

    /**
     * 生成支付二维码图片（前端 img 标签调用，不需要登录）
     * 自动检测服务器局域网 IP，确保手机扫码后能访问
     */
    @GetMapping("/qrcode/{token}")
    public void qrcode(@PathVariable String token, HttpServletResponse response) throws Exception {
        String lanIp = getLanIp();
        String url = "http://" + lanIp + ":" + serverPort + "/api/payment/page/" + token;

        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 2);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(url, BarcodeFormat.QR_CODE, 250, 250, hints);

        response.setContentType("image/png");
        response.setHeader("Cache-Control", "no-cache, no-store");
        MatrixToImageWriter.writeToStream(matrix, "PNG", response.getOutputStream());
    }

    /**
     * 支付确认页面（手机扫码后打开，不需要登录）
     */
    @GetMapping("/page/{token}")
    public String page(@PathVariable String token) {
        PaymentSession session = sessions.get(token);
        if (session == null) {
            return "<!DOCTYPE html><html lang=\"zh-CN\"><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\"><title>支付已过期</title><style>*{margin:0;padding:0;box-sizing:border-box}body{font-family:-apple-system,sans-serif;background:#FAFAFA;display:flex;justify-content:center;align-items:center;min-height:100vh}.card{background:#FFF;border-radius:12px;padding:32px 24px;box-shadow:0 1px 3px rgba(0,0,0,0.1);text-align:center;max-width:320px;width:90%}</style></head><body><div class=\"card\"><p style=\"color:#DC2626;font-size:16px;font-weight:600;\">支付会话已过期</p><p style=\"color:#666;font-size:14px;margin-top:8px;\">请返回订单页面重新生成二维码</p></div></body></html>";
        }

        String amountStr = session.amount.stripTrailingZeros().toPlainString();
        String methodName = session.paymentMethod;

        return "<!DOCTYPE html>\n" +
            "<html lang=\"zh-CN\">\n" +
            "<head>\n" +
            "<meta charset=\"UTF-8\">\n" +
            "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0,user-scalable=no\">\n" +
            "<title>模拟支付</title>\n" +
            "<style>\n" +
            "*{margin:0;padding:0;box-sizing:border-box}\n" +
            "body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;background:#FAFAFA;display:flex;justify-content:center;align-items:center;min-height:100vh}\n" +
            ".card{background:#FFF;border-radius:16px;padding:36px 28px;box-shadow:0 2px 8px rgba(0,0,0,0.08);text-align:center;max-width:340px;width:90%}\n" +
            ".label{font-size:14px;color:#9CA3AF;margin-bottom:4px}\n" +
            ".amount{font-size:42px;font-weight:700;color:#1A1A1A;margin:8px 0}\n" +
            ".method{display:inline-block;background:#F3F4F6;color:#666;font-size:14px;padding:4px 14px;border-radius:20px;margin-bottom:28px}\n" +
            ".btn{display:block;width:100%;padding:14px;border:none;border-radius:10px;font-size:17px;font-weight:600;cursor:pointer;margin-bottom:10px;transition:opacity .2s}\n" +
            ".btn:active{opacity:.8}\n" +
            ".btn-primary{background:#6B4BFF;color:#FFF}\n" +
            ".btn-secondary{background:#F3F4F6;color:#666}\n" +
            ".toast{color:#16A34A;font-size:15px;font-weight:600;margin-top:16px;display:none}\n" +
            ".toast.show{display:block}\n" +
            "</style>\n" +
            "</head>\n" +
            "<body>\n" +
            "<div class=\"card\">\n" +
            "<p class=\"label\">管兴订单系统 · 模拟支付</p>\n" +
            "<div class=\"amount\">¥" + amountStr + "</div>\n" +
            "<div class=\"method\">" + methodName + " 支付</div>\n" +
            "<button class=\"btn btn-primary\" id=\"payBtn\" onclick=\"confirmPayment()\">确认支付</button>\n" +
            "<button class=\"btn btn-secondary\" onclick=\"history.back()\">取消</button>\n" +
            "<p class=\"toast\" id=\"toast\">✓ 支付成功！请返回订单页面</p>\n" +
            "</div>\n" +
            "<script>\n" +
            "function confirmPayment(){\n" +
            "  document.getElementById('payBtn').disabled=true;\n" +
            "  document.getElementById('payBtn').textContent='处理中...';\n" +
            "  fetch('/api/payment/confirm/" + token + "',{method:'POST'})\n" +
            "    .then(function(r){return r.json()})\n" +
            "    .then(function(data){\n" +
            "      if(data.code===200){\n" +
            "        document.getElementById('payBtn').style.display='none';\n" +
            "        document.getElementById('toast').classList.add('show');\n" +
            "        document.querySelector('.btn-secondary').textContent='关闭';\n" +
            "      }\n" +
            "    });\n" +
            "}\n" +
            "</script>\n" +
            "</body>\n" +
            "</html>";
    }

    /**
     * 确认支付（手机页面调用，不需要登录）
     */
    @PostMapping("/confirm/{token}")
    public Result<String> confirm(@PathVariable String token) {
        PaymentSession session = sessions.get(token);
        if (session == null) {
            return Result.error("支付会话不存在或已过期");
        }
        if ("CONFIRMED".equals(session.status)) {
            return Result.success("OK");
        }
        session.status = "CONFIRMED";
        return Result.success("OK");
    }
}
