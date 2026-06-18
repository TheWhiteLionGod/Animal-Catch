from flask import Flask, request, jsonify, Response
from dbhandler import SmartCursor, createAnimalTable, getAnimalStat
from PIL import Image
import io
import os

type APIReturn = tuple[Response, int | None]
ALLOWED_EXTENSIONS: set[str] = {'png', 'jpg', 'jpeg'}

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
    
    # TODO: Identify Animal
    _image: Image = Image.open(io.BytesIO(file.read())).convert("RGB")
    animalName: str = "Lapris"

    return jsonify({
        "success": True,
        "animalName": animalName.lower()
    })

def isFileAllowed(filename: str) -> bool:
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

if __name__ == '__main__':
    app.run(debug=True, host="0.0.0.0", port=5000)
    del cursor
