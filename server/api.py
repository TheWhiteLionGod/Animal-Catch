from flask import Flask, jsonify, Response
from dbhandler import SmartCursor, createAnimalTable, getAnimalStat
import os

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
def homepage() -> Response:
    return jsonify({
        "success": True
    })

@app.route("/getstats/<animalName>")
def getStats(animalName: str) -> Response:
    try:
        stats = getAnimalStat(cursor, animalName)
        return jsonify(stats)
    except ValueError:
        # TODO: Create Animal via AI
        pass
    
    return jsonify({
        "name": animalName,
        "hp": -1,
        "atk": -1,
        "def": -1,
        "spd": -1
    })

if __name__ == '__main__':
    app.run(debug=True, port=5000)
    del cursor
