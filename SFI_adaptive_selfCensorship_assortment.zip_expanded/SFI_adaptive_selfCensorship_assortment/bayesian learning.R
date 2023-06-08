#library
library(tidyverse)
#read document
agents = read.csv("learntest3agentresults.txt", header = TRUE, sep = ",", quote = "\"",
                  dec = ".", fill = TRUE, comment.char = "%", row.names=NULL)

#Visualization of results
#num of people that signaled
ConSignal = agents %>%
  filter(signal == 1) %>%
  group_by(Seed, Timestep,priorCon) %>%
  summarize(ConSignalers = n()) %>%
  mutate(priorCon = as.factor(priorCon))

ggplot(ConSignal, aes(x=Timestep,y=ConSignalers,color = priorCon)) + 
  geom_line() + theme_bw()

agents$priorCon = as.factor(agents$priorCon)
agents$AgentID = as.factor(agents$AgentID)
ggplot(agents[agents$AgentID == 1,], aes(x=Timestep,y=signalProb,color = priorCon)) + 
  geom_line() + theme_bw()
# mean of con agents perception of gov threshold

