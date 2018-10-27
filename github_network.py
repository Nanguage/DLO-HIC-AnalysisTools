#author 4A69616E67
import requests
import sys
import argparse

def getargs():
    parser = argparse.ArgumentParser(description="github api")
    parser.add_argument('-c', '--account', help="your github account")
    parser.add_argument('-w', '--password', help="your github password")
    parser.add_argument('-l', '--language', help='the language you want to search')
    parser.add_argument('-g', '--page', help='the number of page you want to show')
    parser.add_argument('-p', '--prefix', help='the prefix of output')
    return parser.parse_args()
#per_page=100 print 100 item each page
#page=1 print the first page
#language:java search language is java
BASE_URL = 'https://api.github.com'
Args=getargs()
account=Args.account
password = Args.password
prefix=Args.prefix
language_condition=Args.language if Args.language else "Python"
page_condition="page="+Args.page if Args.page else "page=1"
sort_condition='sort=stars'
per_page_condition='per_page=100'
url='https://api.github.com/search/repositories?q=language:'+language_condition+'&'+sort_condition+'&'+per_page_condition+'&'+page_condition
response_dict = requests.get(url).json()
contributors_hash=dict()
for i in range(len(response_dict['items'])):
    owner=response_dict['items'][i]['owner']
    stargazers_count=response_dict['items'][i]['stargazers_count']
    contributors_url=response_dict['items'][i]['contributors_url']
    project_name=response_dict['items'][i]['name']
    contributors_hash[project_name]=[]
    contributors_requests=requests.get(contributors_url+'?'+per_page_condition,auth=(account,password))
    if(contributors_requests.status_code==403):
        continue
    contributors_dict=contributors_requests.json()
    for j in range(len(contributors_dict)):
        contributions=contributors_dict[j]['contributions']
        if int(contributions)>10:
            contributors_hash[project_name].append(contributors_dict[j]['login'])
    print("Project:"+project_name)
    print("Owner:"+owner['login'])
    print("Star:"+str(stargazers_count))
    print("Language:"+language_condition)
    print("Contributors:"),
    for j in range(len(contributors_hash[project_name])-1):
        print(contributors_hash[project_name][j]+","),
    if(len(contributors_hash[project_name])>=1):
        print(contributors_hash[project_name].pop()),
    print
    
