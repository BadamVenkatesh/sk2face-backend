from pydantic_settings import BaseSettings, SettingsConfigDict
from pydantic import Field
import os

class Settings(BaseSettings):
    app_name: str = "ML Service"
    base_dir: str = "/home/badam/Downloads/tempi"
    
    @property
    def model_path(self) -> str:
        return os.path.join(self.base_dir, "saved_model", "sketch_photo_embedding_model.keras")
        
    @property
    def db_path(self) -> str:
        return os.path.join(self.base_dir, "database_embeddings.pkl")

settings = Settings()
