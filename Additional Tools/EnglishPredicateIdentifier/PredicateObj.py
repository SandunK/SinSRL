from flair.data import Sentence
from flair.models import SequenceTagger

tagger = SequenceTagger.load('frame')


# class GetPredicate:
#     # load the NER tagger
#     def __init__(self):
#         self.tagger = SequenceTagger.load('frame')

def getPredicates(sentenceStr):
    # make a sentence
    sentence = Sentence(sentenceStr)
    # run NER over sentence
    tagger.predict(sentence)
    return sentence.to_tagged_string()
