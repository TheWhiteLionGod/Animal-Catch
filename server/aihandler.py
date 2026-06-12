import os

def grabPrompt(filename: str) -> str:
    if not os.path.exists(f"prompts/{filename}.txt"):
        raise FileNotFoundError(f"Unable to Find File prompts/{filename}.txt")
    
    with open(f"prompts/{filename}.txt", 'r') as f:
        return f.read()

if __name__ == '__main__':
    prompt: str = grabPrompt("test")
    print(prompt)