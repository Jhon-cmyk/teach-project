import json
import urllib.error
import urllib.request


DEEPSEEK_CHAT_URL = "https://api.deepseek.com/chat/completions"


def stream_deepseek(api_key, system_prompt, user_prompt, max_tokens=4000, temperature=0.4):
    if not api_key:
        raise RuntimeError("DeepSeek api key is missing")

    payload = {
        "model": "deepseek-chat",
        "stream": True,
        "temperature": temperature,
        "max_tokens": max_tokens,
        "messages": [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt},
        ],
    }

    req = urllib.request.Request(
        DEEPSEEK_CHAT_URL,
        data=json.dumps(payload, ensure_ascii=False).encode("utf-8"),
        headers={
            "Content-Type": "application/json",
            "Authorization": "Bearer " + api_key,
        },
        method="POST",
    )

    try:
        with urllib.request.urlopen(req, timeout=300) as resp:
            for raw in resp:
                line = raw.decode("utf-8", errors="ignore").strip()
                if not line.startswith("data: ") or line == "data: [DONE]":
                    continue
                data = json.loads(line[6:])
                content = (
                    data.get("choices", [{}])[0]
                    .get("delta", {})
                    .get("content")
                )
                if content:
                    yield content
    except urllib.error.HTTPError as exc:
        body = exc.read().decode("utf-8", errors="ignore")
        raise RuntimeError("DeepSeek HTTP %s: %s" % (exc.code, body[:500]))

