#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

function_setup="You are an AI assistant with access to real-time data through an API endpoint.To look up data, you can make HTTP GET requests to: http://java-service:8081/api/data/lookup/{query}  Available queries: - temperature: Gets current temperature - stock: Gets current stock price             When you need real-time data:           1. Make the appropriate HTTP GET request            2. Include the data in your response            3. Always specify the timestamp of the data   4. Just give me a concise response, I don't need an explaination of how you're retreiving the data.            Example:            To get temperature: GET http://java-service:8081/api/data/lookup/temperature                        To get stock prices: GET http://java-service:8081/api/data/lookup/stock            Remember to interpret and explain the data in your responses and if you are unable to get real time data don't make up answers and say the datasource was unavailable.   "

send_query() {
    local prompt="$1"
    local full_prompt="${function_setup} $prompt"

    echo -e "\n${BLUE}Sending query:${NC} $prompt"
    echo -e "${BLUE}Timestamp:${NC} $(date)"

    curl -s -X POST http://192.168.128.142:11434/api/generate \
        -H "Content-Type: application/json" \
        -d "{
                \"model\": \"llama3.2:3b\",
                \"prompt\": \"$full_prompt\",
                \"stream\": false
        }" | jq -r .response

    echo -e "\n${GREEN}------------------------${NC}"
}



echo "Starting POC Demo..."
echo "Ensuring services are ready..."
sleep 5



# Basic queries
send_query "What's the current temperature?"
sleep 2

send_query "Can you check the current stock price?"
sleep 2

send_query "Please tell me both the temperature and the stock price, and analyze if there might be any correlation."
sleep 2

# Complex query with conditional logic
send_query "What's the temperature like? If it's above 25°C, suggest some indoor activities."
sleep 2

# Trend analysis
echo -e "\n${BLUE}Executing trend analysis...${NC}"
for i in {1..3}; do
    echo -e "\n${BLUE}Reading $i of 3${NC}"
    send_query "What's the current stock price?"
    if [ $i -lt 3 ]; then
        echo "Waiting 10 seconds for next reading..."
        sleep 10
    fi
done