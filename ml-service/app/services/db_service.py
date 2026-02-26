import pickle
import numpy as np
from sklearn.metrics.pairwise import cosine_similarity
from app.core.config import settings

_database = None

def load_db():
    global _database
    with open(settings.db_path, "rb") as f:
        _database = pickle.load(f)

def retrieve_top_k(query_emb, k=3):
    if _database is None:
        raise RuntimeError("Database is not loaded")
        
    filenames = list(_database.keys())
    embeddings = np.array([_database[fn]['embedding'] for fn in filenames])
    
    sims = cosine_similarity(query_emb, embeddings)[0]
    top_k_idx = np.argsort(sims)[::-1][:k]
    
    results = [
        {"filename": filenames[i], "score": float(sims[i])}
        for i in top_k_idx
    ]
    return results
