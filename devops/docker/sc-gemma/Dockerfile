FROM vllm/vllm-openai:latest

WORKDIR /app

COPY ./gemma-3-270m ./gemma-3-270m
COPY ./chat_template.jinja ./chat_template.jinja

EXPOSE 8000

CMD ["--model", "/app/gemma-3-270m", \
     "--gpu-memory-utilization", "0.9", \
     "--chat-template", "/app/chat_template.jinja" \
    ]