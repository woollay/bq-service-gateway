#!/bin/bash
PORTS=(9992 9982 9972)
IMAGE=biuqu/bq-gateway:1.

sh build.sh "${PORTS[@]}" "$IMAGE"