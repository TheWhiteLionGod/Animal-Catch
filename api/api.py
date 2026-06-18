from flask import Flask, request, jsonify, Response
from dbhandler import SmartCursor, createAnimalTable, getAnimalStat
import requests
import os

type APIReturn = tuple[Response, int | None]
ALLOWED_EXTENSIONS: set[str] = {'png', 'jpg', 'jpeg'}
INTERNAL_SERVER: str = os.environ.get("INTERNAL_SERVER")

app = Flask(__name__)

cursor = SmartCursor(
    os.environ.get("DBHOST"),
    os.environ.get("DBNAME"),
    os.environ.get("DBUSER"),
    os.environ.get("DBPASS"),
    os.environ.get("DBPORT")
)
createAnimalTable(cursor)

@app.route("/")
def homepage() -> APIReturn:
    return jsonify({
        "success": True
    })

@app.route("/getstats/<animalName>")
def getStats(animalName: str) -> APIReturn:
    try:
        stats = getAnimalStat(cursor, animalName.lower())
        return jsonify({
            "success": True,
            **stats
        })

    except ValueError:
        # TODO: Create Animal via AI
        pass
    
    return jsonify({
        "success": False,
        "name": animalName.lower(),
        "hp": -1,
        "atk": -1,
        "def": -1,
        "spd": -1
    }), 400

@app.route("/identify", methods=["POST"])
def identifyAnimal() -> APIReturn:
    if 'image' not in request.files:
        return jsonify({
            "success": False
        }), 400
    
    file = request.files['image']
    if file.filename == '' or not isFileAllowed(file.filename):
        return jsonify({
            "success": False
        }), 400
    
    try:
        response = requests.post(
            INTERNAL_SERVER + "/identify", 
            files={"image": (file.filename, file.stream, file.mimetype)},
            timeout=15
        )

        return jsonify(response.json()), response.status_code

    except requests.RequestException as e:
        raise e
        return jsonify({
            "success": False
        }), 400

def isFileAllowed(filename: str) -> bool:
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

if __name__ == '__main__':
    app.run(host="0.0.0.0", port=5000)
    del cursor
