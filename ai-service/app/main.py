from __future__ import annotations

import os
import time
from dataclasses import dataclass, field
from typing import Any

import httpx
from fastapi import FastAPI, File, HTTPException, UploadFile
from pydantic import BaseModel, Field


class ChatRequest(BaseModel):
    question: str = Field(min_length=1)


class ChatResponse(BaseModel):
    answer: str
    references: list[str] = []
    configured: bool = True


class DocumentResponse(BaseModel):
    id: int
    title: str
    source_type: str
    status: str
    created_at: float


@dataclass
class KnowledgeStore:
    documents: list[DocumentResponse] = field(default_factory=list)
    chunks: list[dict[str, Any]] = field(default_factory=list)

    def __post_init__(self) -> None:
        if not self.documents:
            self.add_seed("智能位移传感器技术说明", "智能位移传感器用于边坡、坝体和矿山结构位移连续监测，量程 300mm，精度 0.1mm，防护等级 IP67。")
            self.add_seed("边坡自动化监测方案白皮书", "边坡自动化监测通常包含位移传感器、雨量计、视频监控、多通道采集仪、4G 网关和太阳能供电箱。")
            self.add_seed("尾矿库安全监测常见问答", "尾矿库监测关注坝体位移、浸润线、库水位、降雨量和视频巡检，报价需按测点与通信条件拆分。")

    def add_seed(self, title: str, content: str) -> DocumentResponse:
        doc = DocumentResponse(
            id=len(self.documents) + 1,
            title=title,
            source_type="SEED",
            status="READY",
            created_at=time.time(),
        )
        self.documents.append(doc)
        self.chunks.append({"doc": title, "content": content})
        return doc

    async def add_upload(self, file: UploadFile) -> DocumentResponse:
        raw = await file.read()
        text = raw.decode("utf-8", errors="ignore")[:4000] or f"{file.filename} 已上传，等待解析。"
        doc = DocumentResponse(
            id=len(self.documents) + 1,
            title=file.filename or "未命名文档",
            source_type="UPLOAD",
            status="READY",
            created_at=time.time(),
        )
        self.documents.append(doc)
        self.chunks.append({"doc": doc.title, "content": text})
        return doc

    def search(self, question: str) -> list[dict[str, Any]]:
        words = set(question.lower().replace("，", " ").replace("？", " ").split())
        scored = []
        for chunk in self.chunks:
            content = chunk["content"].lower()
            score = sum(1 for word in words if word and word in content)
            if score or not scored:
                scored.append((score, chunk))
        scored.sort(key=lambda item: item[0], reverse=True)
        return [chunk for _, chunk in scored[:3]]


store = KnowledgeStore()
app = FastAPI(title="Product Show AI Knowledge Service", version="0.1.0")


@app.get("/ai/health")
async def health() -> dict[str, str]:
    return {"status": "UP", "service": "ai-service"}


@app.get("/ai/documents", response_model=list[DocumentResponse])
async def documents() -> list[DocumentResponse]:
    return store.documents


@app.post("/ai/documents", response_model=DocumentResponse)
async def upload_document(file: UploadFile = File(...)) -> DocumentResponse:
    return await store.add_upload(file)


@app.post("/ai/search")
async def search(request: ChatRequest) -> dict[str, Any]:
    hits = store.search(request.question)
    return {"hits": hits}


@app.post("/ai/chat", response_model=ChatResponse)
async def chat(request: ChatRequest) -> ChatResponse:
    hits = store.search(request.question)
    references = [hit["doc"] for hit in hits]
    context = "\n".join(f"- {hit['doc']}: {hit['content']}" for hit in hits)
    api_key = os.getenv("DEEPSEEK_API_KEY", "")

    if not api_key:
        return ChatResponse(
            answer=f"DEEPSEEK_API_KEY 尚未配置。已根据本地知识库检索到参考资料：{', '.join(references)}。问题：{request.question}",
            references=references,
            configured=False,
        )

    payload = {
        "model": "deepseek-chat",
        "messages": [
            {"role": "system", "content": "你是工程监测产品与报价助手。回答要简洁、专业，并引用知识库上下文。"},
            {"role": "user", "content": f"知识库上下文：\n{context}\n\n问题：{request.question}"},
        ],
        "temperature": 0.2,
    }
    headers = {"Authorization": f"Bearer {api_key}", "Content-Type": "application/json"}

    async with httpx.AsyncClient(timeout=30) as client:
        response = await client.post("https://api.deepseek.com/chat/completions", json=payload, headers=headers)
    if response.status_code >= 400:
        raise HTTPException(status_code=502, detail=f"DeepSeek API error: {response.text}")
    data = response.json()
    answer = data["choices"][0]["message"]["content"]
    return ChatResponse(answer=answer, references=references)

