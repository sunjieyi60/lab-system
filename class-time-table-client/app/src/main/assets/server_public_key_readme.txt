设备 REGISTER 时使用服务端 RSA 公钥加密 AES 密钥，请在此目录放置服务端公钥之一：

1. server_public_key.pem
   - 内容为 PEM 格式：-----BEGIN PUBLIC KEY----- ... -----END PUBLIC KEY-----

2. server_public_key_base64.txt
   - 内容为 X.509 SubjectPublicKeyInfo 的 Base64 编码（单行，无换行）

生成方式（服务端已有私钥时）：
  openssl rsa -in server_private.pem -pubout -out server_public.pem
  将 server_public.pem 内容放入 server_public_key.pem，或将其 Base64 放入 server_public_key_base64.txt

未放置公钥时，REGISTER 会失败并打日志。
