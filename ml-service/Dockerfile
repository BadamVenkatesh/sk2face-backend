FROM ghcr.io/astral-sh/uv:python3.12-bookworm-slim

WORKDIR /app

# Enable bytecode compilation and disable caching for uv
ENV UV_COMPILE_BYTECODE=1

# Place the virtual environment inside the project directory
ENV UV_PROJECT_ENVIRONMENT=/app/.venv
# Ensure the virtualenv is on the PATH
ENV PATH="/app/.venv/bin:$PATH"

# Install dependencies separately to leverage Docker layer caching and BuildKit cache
RUN --mount=type=cache,target=/root/.cache/uv \
    --mount=type=bind,source=uv.lock,target=uv.lock \
    --mount=type=bind,source=pyproject.toml,target=pyproject.toml \
    uv sync --frozen --no-install-project --no-dev

# Copy the rest of the application code
COPY ./app /app/app
COPY pyproject.toml uv.lock ./

# Sync the project itself (without dev dependencies)
RUN --mount=type=cache,target=/root/.cache/uv \
    uv sync --frozen --no-dev

ENV BASE_DIR=/app/data

EXPOSE 9090

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "9090"]
