from fastapi import APIRouter, HTTPException, Request
from pydantic import BaseModel
import os
import numpy as np

from app.services.image_service import preprocess_image
from app.services.model_service import predict_embeddings
from app.services.db_service import retrieve_top_k

router = APIRouter()

class MatchRequest(BaseModel):
    image_path: str   # <-- changed from image_url

@router.get("/health")
def health():
    return {"status": "ok"}

@router.post("/match")
async def match_image(req: MatchRequest, request: Request):

    try:
        # ✅ Check if file exists
        if not os.path.exists(req.image_path):
            raise HTTPException(status_code=400, detail="Image path does not exist")

        # ✅ Preprocess directly from local file
        img_array = preprocess_image(req.image_path)
        img_batch = np.expand_dims(img_array, axis=0)

        # ✅ Predict embeddings
        query_emb = predict_embeddings(img_batch)

        # ✅ Retrieve top matches
        results = retrieve_top_k(query_emb, k=3)

        # ✅ Construct static URLs dynamically
        host = request.url.hostname
        port = request.url.port

        if port:
            domain = f"{request.url.scheme}://{host}:{port}"
        else:
            domain = f"{request.url.scheme}://{host}"

        urls = [
            f"{domain}/static/photos/{r['filename']}"
            for r in results
        ]

        return {"matches": urls}

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))