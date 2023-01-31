#purpose: explore homophily, how it works and why

#library
library(tidyverse)
library(wesanderson)
library(viridisLite)
library(viridis)  
library(plyr)
library(ggridges)

#read files
#agents = read.csv("~/Documents/Research/self-censorship/SFI_adaptive_selfCensorship_assortment.zip_expanded/SFI_adaptive_selfCensorship_assortment/homophily_test1000agentresults.txt",
agents = read.csv("~/Documents/Research/self-censorship/Archive/src/structures/homophily_test3agentresults.txt",
                  comment.char = "%")

# timecourse of learning for a few agents
ggplot(agents[agents$AgentID == 1,],
       aes(x = Timestep, y = signalProb, color = ValueHi, linetype = factor(homophily),
           group = Agent)) +  geom_line()

# get the data into the right form
agents1 = agents %>%
  mutate(belief = ifelse(ValueHi == 'true','Con','Pro')) %>%
  select(Seed, Timestep, homophily, highRatio, belief, signalProb, fitness)

# start by looking at the last timestep
agents.end = agents1 %>%
  filter(Timestep == 5000) %>%
  mutate(Seed = as.factor(Seed))

ggplot(agents.end, aes(x = signalProb, y = Seed, color = belief)) +
  geom_density_ridges() +
  theme_ridges() + 
  theme(legend.position = "none") +
  facet_grid(rows = vars(homophily), cols = vars(highRatio))

# I want to see what a normal histogram of a single replicate looks like
agents.peek = agents1 %>%
  filter(homophily == 1, highRatio == .5) %>%
  mutate(signalProb = as.numeric(signalProb))

ggplot(agents[agents$Timestep == max(agents$Timestep),],
       aes(x = signalProb, color = ValueHi)) +
  geom_histogram() + facet_grid(cols = vars(homophily), rows = vars(Seed))
