#!/bin/bash
pip install -r requirements.txt
gunicorn server:app --bind 0.0.0.0:5000
