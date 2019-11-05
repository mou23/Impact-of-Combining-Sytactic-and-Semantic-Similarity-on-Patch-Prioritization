import javalang
import sys

content = " ".join(sys.argv[1:])
# print(content)

tokens = list(javalang.tokenizer.tokenize(content))
# print(tokens)
output = ""
for i in range(0,len(tokens)):
    if i!=len(tokens)-1:
        output = output + tokens[i].value+"\n" #+ str(type(tokens[j]))+"\n"
    else:
        output = output + tokens[i].value+"\n\n"
print(output)

# for i in range(0,len(content)):
#     tokens = list(javalang.tokenizer.tokenize(content[i]))
#     if(len(tokens)==0):
#         output = output + "empty"+"\n\n"
#     for j in range(0,len(tokens)):
#         if j!=len(tokens)-1:
#             output = output + tokens[j].value+"\n" #+ str(type(tokens[j]))+"\n"
#         else:
#             output = output + tokens[j].value+"\n\n"
        
