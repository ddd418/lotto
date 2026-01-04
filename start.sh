#!/bin/bash
python init_db.py
exec uvicorn api_server:app --host 0.0.0.0 --port ${PORT:-8000}
