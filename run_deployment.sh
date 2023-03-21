#!/bin/bash

function fail {
  printf '%s\n' "$1" >&2
  exit "${2-1}"
}

ls deployment.yaml || fail "Could not find deployment file. Run script as ./run_deployment.sh"

echo "Will run deployment from above file..."

# Configure kubectl config to use dev context
echo "Configure dev context"

kubectl delete -f deployment.yaml || exit

# apply new deployment pulling latest container from ecr
echo "Applying new deployment..."
kubectl apply -f deployment.yaml || exit

echo "Deployment complete"