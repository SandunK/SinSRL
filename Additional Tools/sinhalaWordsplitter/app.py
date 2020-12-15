from flask import Flask, redirect, url_for, request, render_template
import sys

sys.path.insert(0, "/home/mgsk_gunasekara/sinhalaWordsplitter/sinling")
sys.path.append('..sinling')
from sinling import word_splitter

app = Flask(__name__)


@app.route('/', methods=['GET'])
def main():
    return 'Request Success'


@app.route('/split', methods=['POST'])
def split():
    query_request = request.json
    print (query_request)
    results = word_splitter.split(query_request["word"])
    return results


if __name__ == '__main__':
    app.run(debug=True)
