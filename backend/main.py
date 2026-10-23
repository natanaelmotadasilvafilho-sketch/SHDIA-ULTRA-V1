from fastapi import FastAPI, UploadFile, File, Form, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from uuid import uuid4
from pathlib import Path
from datetime import datetime, timezone
import shutil
import os

APP_DIR = Path(__file__).parent
UPLOAD_DIR = APP_DIR / "temporary_uploads"
UPLOAD_DIR.mkdir(exist_ok=True)
MAX_IMAGE_BYTES = 20 * 1024 * 1024
ALLOWED_TYPES = {"image/jpeg", "image/png", "image/webp"}

app = FastAPI(title="SHDIA ULTRA Cloud API", version="2.0.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Em produção, restringir aos domínios/origens esperados.
    allow_credentials=False,
    allow_methods=["GET", "POST"],
    allow_headers=["*"],
)

JOBS = {}

class JobResponse(BaseModel):
    job_id: str
    status: str
    mode: str
    style: str
    message: str

@app.get("/health")
def health():
    return {
        "ok": True,
        "service": "SHDIA ULTRA Cloud API",
        "version": "2.0.0",
        "ai_model_integrated": False,
        "message": "API online; conecte um modelo real de IA antes de produção."
    }

@app.post("/jobs", response_model=JobResponse)
async def create_job(
    file: UploadFile = File(...),
    style: str = Form("anime"),
    upscale: str = Form("2x"),
):
    if file.content_type not in ALLOWED_TYPES:
        raise HTTPException(415, "Formato não suportado. Envie JPG, PNG ou WEBP.")
    if style not in {"anime", "live_action", "restore"}:
        raise HTTPException(400, "Estilo inválido.")
    if upscale not in {"2x", "4x"}:
        raise HTTPException(400, "Ampliação inválida.")

    job_id = str(uuid4())
    suffix = {"image/jpeg": ".jpg", "image/png": ".png", "image/webp": ".webp"}[file.content_type]
    destination = UPLOAD_DIR / f"{job_id}{suffix}"
    size = 0
    try:
        with destination.open("wb") as out:
            while True:
                chunk = await file.read(1024 * 1024)
                if not chunk:
                    break
                size += len(chunk)
                if size > MAX_IMAGE_BYTES:
                    out.close()
                    destination.unlink(missing_ok=True)
                    raise HTTPException(413, "Arquivo excede o limite de 20 MB.")
                out.write(chunk)
    finally:
        await file.close()

    # Modo demonstrativo: não afirma que a imagem foi melhorada.
    # Troque por uma fila/worker que execute um modelo real de restauração/upscaling.
    JOBS[job_id] = {
        "job_id": job_id,
        "status": "waiting_for_model",
        "mode": "cloud",
        "style": style,
        "upscale": upscale,
        "filename": destination.name,
        "bytes": size,
        "created_at": datetime.now(timezone.utc).isoformat(),
        "result_url": None,
        "demo_mode": True,
        "message": "Upload recebido. O modelo de IA ainda não está integrado."
    }
    return JobResponse(
        job_id=job_id,
        status="waiting_for_model",
        mode="cloud",
        style=style,
        message="Arquivo recebido; integração com IA real ainda pendente."
    )

@app.get("/jobs/{job_id}")
def get_job(job_id: str):
    job = JOBS.get(job_id)
    if not job:
        raise HTTPException(404, "Trabalho não encontrado.")
    return job

@app.delete("/jobs/{job_id}")
def delete_job(job_id: str):
    job = JOBS.pop(job_id, None)
    if not job:
        raise HTTPException(404, "Trabalho não encontrado.")
    path = UPLOAD_DIR / job["filename"]
    path.unlink(missing_ok=True)
    return {"ok": True, "message": "Arquivo temporário removido."}
