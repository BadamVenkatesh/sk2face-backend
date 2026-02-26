from fastapi import FastAPI
from fastapi.staticfiles import StaticFiles
from app.api.routes import router
from app.services.model_service import init_model
from app.services.db_service import load_db
import os
from app.client.eureka_client import register_with_eureka

app = FastAPI(title="ML Service", description="A FastAPI service for image matching.")

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
# Go one level up → project root → then into data/
DATA_DIR = os.path.abspath(os.path.join(BASE_DIR, "..", "data"))

print("BASE_DIR:", BASE_DIR)
print("DATA_DIR:", DATA_DIR)
print("Photos folder exists:", os.path.exists(os.path.join(DATA_DIR, "photos")))
print("File exists:", os.path.exists(os.path.join(DATA_DIR, "photos", "m1-004-01.jpg")))


@app.on_event("startup")
async def startup_event():
    print("Loading model...")
    init_model()
    print("Model loaded!")
    
    print("Loading database...")
    load_db()
    print("Database loaded!")

    print("Register with eureka...")
    await register_with_eureka()
    print("Eureka Registered!")

app.include_router(router)

# Ensure the data directory exists to prevent errors on mount if volume is missing
if not os.path.exists(DATA_DIR):
    os.makedirs(os.path.join(DATA_DIR, "photos"), exist_ok=True)

# Mount the static directory to serve images
app.mount("/static", StaticFiles(directory=DATA_DIR, follow_symlink=True), name="static")
