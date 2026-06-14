import psycopg2

class SmartCursor:
    def __init__(self, host: str, db: str, user: str, password: str, port: str) -> None:
        self.connection = psycopg2.connect(
            host=host,
            database=db,
            user=user,
            password=password,
            port=port
        )

        self.cursor = self.connection.cursor()
    
    def __del__(self) -> None:
        if hasattr(self, 'cursor') and self.cursor:
            self.cursor.close()
        
        if hasattr(self, 'connection') and self.connection:
            self.connection.close()

def createAnimalTable(smartCursor: SmartCursor) -> None:
    smartCursor.cursor.execute(
        """
        CREATE TABLE IF NOT EXISTS animals (
            id SERIAL PRIMARY KEY,
            name VARCHAR(100) UNIQUE NOT NULL,
            hp INT NOT NULL,
            atk INT NOT NULL,
            def INT NOT NULL,
            spd INT NOT NULL
        );
        """
    )

    # FOR TESTING: REMOVE FOR RELEASE
    smartCursor.cursor.execute(
        """
        SELECT * FROM animals WHERE name = 'Lapris';
        """
    )

    row = smartCursor.cursor.fetchone()
    if row is None:
        smartCursor.cursor.execute(
            """
            INSERT INTO animals (name, hp, atk, def, spd) VALUES (%s, %s, %s, %s, %s);
            """,
            ("Lapris", 100, 120, 75, 95)
        )

    smartCursor.connection.commit()

def getAnimalStat(smartCursor: SmartCursor, animalName: str) -> dict[str, str | int]:
    smartCursor.cursor.execute(
        """
        SELECT * FROM animals WHERE name = %s;
        """, 
        (animalName,)
    )

    row = smartCursor.cursor.fetchone()
    if row is None:
        raise ValueError("Animal Does Not Exist in Database")
    
    return {
        "name": row[1],
        "hp": row[2],
        "atk": row[3],
        "def": row[4],
        "spd": row[5]
    }
