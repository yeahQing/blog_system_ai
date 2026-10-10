# Ollama Embedding 向量生成接口文档

# Ollama Embedding 向量生成接口文档

## 1\. 基础信息

|项目|内容|
|---|---|
|接口地址|[http://82\.157\.202\.129:11434/api/embed](http://82.157.202.129:11434/api/embed)|
|请求方式|POST|
|数据格式|Request/Response：JSON|
|请求头|Content\-Type: application/json|

## 2\. 请求参数（Body JSON）

|参数名|类型|是否必填|说明|示例|
|---|---|---|---|---|
|model|string|是|向量模型名称，服务端已部署 bge\-m3|`"bge-m3"`|
|input|string / string\[\]|是|需要向量化的文本，支持单文本 / 批量多文本|单文本：`"test"`；批量：`["文本1","文本2"]`|

### 请求示例（原始 JSON）

```json
{
    "model": "bge-m3",
    "input": "test"
}
```

### PowerShell 请求示例

```powershell
Invoke-RestMethod -Uri "http://82.157.202.129:11434/api/embed" -Method Post -Body '{"model":"bge-m3","input":"test"}' -ContentType "application/json"
```

### cURL 请求示例

```bash
curl http://82.157.202.129:11434/api/embed \
-X POST \
-H "Content-Type: application/json" \
-d '{"model":"bge-m3","input":"test"}'
```

## 3\. 返回参数说明

|参数名|类型|说明|
|---|---|---|
|model|string|当前使用的向量模型|
|embeddings|number\[\]\[\]|文本对应的向量数组，一条输入对应一个浮点向量|
|total\_duration|number|总耗时（纳秒）|
|load\_duration|number|模型加载耗时（纳秒）|
|prompt\_eval\_count|number|文本 token 计数|

### 成功返回示例

```json
{
  "model": "bge-m3",
  "embeddings": [
    [
      0.01256,
      -0.03692,
      0.00871,
      ...
    ]
  ],
  "total_duration": 125600000,
  "load_duration": 62000000,
  "prompt_eval_count": 1
}
```

## 4\. 状态码说明

|HTTP 状态码|含义|常见原因|
|---|---|---|
|200|请求成功|模型正常返回向量|
|400|请求参数错误|model 不存在、input 为空、JSON 格式错误|
|500|服务内部异常|Ollama 服务崩溃 / 模型加载失败 / 显存不足|
|503|服务不可用|Ollama 进程未启动、端口未开放|

## 5\. 调用函数封装（PowerShell）

```powershell
function Get-TextEmbedding {
    param(
        [Parameter(Mandatory=$true)]
        $InputText,
        [string]$Model = "bge-m3",
        [string]$Url = "http://82.157.202.129:11434/api/embed"
    )
    $reqBody = @{
        model = $Model
        input = $InputText
    } | ConvertTo-Json
    try {
        $res = Invoke-RestMethod -Uri $Url -Method Post -Body $reqBody -ContentType "application/json" -TimeoutSec 30
        return $res
    }
    catch {
        Write-Error "接口调用失败：$_"
        return $null
    }
}
# 使用
$vecResult = Get-TextEmbedding -InputText "测试文本"
# 获取向量数组
$vecResult.embeddings
```

## 6\. 注意事项

1. IP 地址为内网 / 服务器固定地址，外部网络无法访问需做端口放行；

2. 批量输入时 `input` 传字符串数组，接口会一次性返回多条向量；

3. 大文本批量调用建议增加超时时间，默认超时 30 秒；

4. 向量维度由 `bge-m3` 模型固定，业务存储直接使用 `embeddings` 数组即可。

> （注：部分内容可能由 AI 生成）
