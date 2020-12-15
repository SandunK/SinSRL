from flask import Flask, request
from flair.data import Sentence
from flair.models import SequenceTagger

app = Flask(__name__)


@app.route('/', methods=['GET'])
def main():
    return 'Request Success'


@app.route('/getpredicates', methods=['POST'])
def getFrames():
    query_request = request.json
    print (query_request)
    sentenceStr = query_request["word"]
    # make a sentence
    sentence = Sentence(sentenceStr)

    # load the NER tagger
    tagger = SequenceTagger.load('frame')

    # run NER over sentence
    tagger.predict(sentence)
    print (sentence.to_tagged_string())
    
    #return str(sentence.to_dict(tag_type='frame'))
    return '{"sentence":"'+sentence.to_tagged_string()+'"}'


if __name__ == '__main__':
    app.run(port=3001)
