from flask import Flask
from flask_restful import Resource, Api, reqparse
from flask import request
from flask import jsonify
import logging
import ast
import pre_processing

app = Flask(__name__)
app.config["DEBUG"] = True
api = Api(app)
# logging.basicConfig(filename='example.log', filemode='w', level=logging.DEBUG)


@app.route('/getbaseword', methods=['POST'])
def get_base_word():
    word = request.json['word']
    # base_word=""
    logging.info(word)
    system_output = pre_processing.analyze_word(pre_processing.preprocess_text(word))[0]
    # logging.info("system_output {}", system_output)
    if isinstance(system_output, tuple):
        # logging.info('system generate an output')
        temp = system_output[0].split("+")[0]
        return jsonify(
            debug=system_output[1].split("+")[0] if "GUESS" in temp else temp,
        )
    else:
        # logging.info('system dosen\'t generate an output')
        return jsonify(
            debug=word,
        )


if __name__ == '__main__':
    app.run()  # run our Flask app
