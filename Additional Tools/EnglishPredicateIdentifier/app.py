from flask import Flask, request
import PredicateObj

app = Flask(__name__)
tagger = PredicateObj


@app.route('/', methods=['GET'])
def main():
    return 'Request Success'


@app.route('/getpredicates', methods=['POST'])
def getFrames():
    query_request = request.json
    print(query_request)
    sentenceStr = query_request["word"]
    predicateString = tagger.getPredicates(sentenceStr)
    # make a sentence
    # sentence = Sentence(sentenceStr)
    # 
    # # load the NER tagger
    # tagger = SequenceTagger.load('frame')
    # 
    # # run NER over sentence
    # tagger.predict(sentence)
    print(predicateString)

    # return str(sentence.to_dict(tag_type='frame'))
    return '{"sentence":"' + predicateString + '"}'


if __name__ == '__main__':
    app.run(port=3001)
