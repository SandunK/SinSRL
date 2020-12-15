from fst_lookup import FST
import re



Nounfst = FST.from_file('./module/Nouns2.fst')
Verbfst = FST.from_file('./module/verbs.fst')
Guesserfst = FST.from_file('./module/NounGuesser.fst')
Adjfst = FST.from_file('./module/Adjectives.fst')
Partfst = FST.from_file('./module/particles.fst')

TAG_RE = re.compile(r'<[^>]+>')

def remove_tags(text):
    return TAG_RE.sub('', text)

def preprocess_text(sen):
    # Removing html tags
    sentence = remove_tags(sen)
    sentence=sentence.replace("\u200b","")
    sentence=sentence.replace("\u200d","")
    sentence=sentence.replace("."," ")
    sentence=sentence.replace(","," ")
    sentence=sentence.replace("?"," ")
    sentence=sentence.replace("\'","")
    sentence=sentence.replace("\"","")
    sentence=sentence.replace("‘","")
    sentence=sentence.replace("’","")


    # Removing multiple spaces
    sentence = re.sub(r'\s+', ' ', sentence)

    return sentence

def analyze_word(word):
    x= list(sorted(Nounfst.analyze(word.strip())))

    x=x+(list(sorted(Adjfst.analyze(word.strip()))))

    x=x+(list(sorted(Partfst.analyze(word.strip()))))


    x=x+(list(sorted(Verbfst.analyze(word.strip()))))
    x = list(filter(None, x))
    if len(x)==0:
        x= list(sorted(Guesserfst.analyze(word.strip())))

    if len(x)==0:
        x.append(word + "+ ?")

    res = list(filter(None, x))



    return res

def analyze(word):
    x= list(sorted(Nounfst.analyze(word.strip())))
    return x