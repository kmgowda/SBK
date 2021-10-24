#
# Copyright (c) KMG. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#

echo "Jit pack setup for SBK"
echo "Default JAVA_HOME :$JAVA_HOME"
echo "Default java : `which java`"
wget https://github.com/sormuras/bach/raw/master/install-jdk.sh
source install-jdk.sh -F 17 -L GPL
echo "Updated JAVA_HOME :$JAVA_HOME"
echo "Updated java : `which java`"