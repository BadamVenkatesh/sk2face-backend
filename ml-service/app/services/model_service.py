import tensorflow as tf
from tensorflow import keras
from app.core.config import settings

_model = None

def build_embedding_model(input_shape=(112, 112, 3), embed_dim=512):
    base = keras.applications.ResNet50(
        include_top=False,
        weights=None,
        input_shape=input_shape,
        pooling='avg'
    )
    inputs = keras.Input(shape=input_shape, name='image_input')
    x = base(inputs, training=False)
    x = keras.layers.Dense(embed_dim, activation='relu', name='embedding_dense')(x)
    outputs = keras.layers.Lambda(
        lambda t: tf.math.l2_normalize(t, axis=1),
        output_shape=(embed_dim,),
        name='l2_norm'
    )(x)
    
    model = keras.Model(inputs, outputs, name='embedding_model')
    return model

def init_model():
    global _model
    _model = build_embedding_model()
    _model.load_weights(settings.model_path)

def predict_embeddings(img_batch):
    if _model is None:
        raise RuntimeError("Model is not initialized")
    return _model.predict(img_batch, verbose=0)
