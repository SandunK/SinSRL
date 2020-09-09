import json
import pandas as pd

with open('outputENGLISH.json','r') as file:
    data = json.load(file)

df_json = pd.DataFrame(data)
df_json

df_json.to_csv('outputCsv.csv')