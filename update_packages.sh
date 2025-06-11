#!/bin/bash

# 更新包名映射
find src/main/java/com/zhouruojun/manus -type f -name "*.java" -exec sed -i '' \
    -e 's/package com\.zhouruojun\.manus\.core\.agent\.base/package com.zhouruojun.manus.domain.agent.base/g' \
    -e 's/package com\.zhouruojun\.manus\.core\.agent\.specialized/package com.zhouruojun.manus.domain.agent.specialized/g' \
    -e 's/package com\.zhouruojun\.manus\.core\.node/package com.zhouruojun.manus.domain.workflow.node/g' \
    -e 's/package com\.zhouruojun\.manus\.core\.node\.base/package com.zhouruojun.manus.domain.workflow.node.base/g' \
    -e 's/package com\.zhouruojun\.manus\.core\.node\.specialized/package com.zhouruojun.manus.domain.workflow.node.specialized/g' \
    -e 's/package com\.zhouruojun\.manus\.core/package com.zhouruojun.manus.domain.workflow.engine/g' \
    -e 's/package com\.zhouruojun\.manus\.model/package com.zhouruojun.manus.domain.model/g' \
    -e 's/package com\.zhouruojun\.manus\.service/package com.zhouruojun.manus.application.service/g' \
    -e 's/package com\.zhouruojun\.manus\.config/package com.zhouruojun.manus.application.config/g' \
    -e 's/package com\.zhouruojun\.manus\.tools/package com.zhouruojun.manus.infrastructure.tools/g' \
    -e 's/package com\.zhouruojun\.manus\.serializers/package com.zhouruojun.manus.infrastructure.serializers/g' \
    -e 's/package com\.zhouruojun\.manus\.exception/package com.zhouruojun.manus.infrastructure.exception/g' \
    {} \;

# 更新导入语句映射
find src/main/java/com/zhouruojun/manus -type f -name "*.java" -exec sed -i '' \
    -e 's/import com\.zhouruojun\.manus\.core\.agent\.base/import com.zhouruojun.manus.domain.agent.base/g' \
    -e 's/import com\.zhouruojun\.manus\.core\.agent\.specialized/import com.zhouruojun.manus.domain.agent.specialized/g' \
    -e 's/import com\.zhouruojun\.manus\.core\.node/import com.zhouruojun.manus.domain.workflow.node/g' \
    -e 's/import com\.zhouruojun\.manus\.core\.node\.base/import com.zhouruojun.manus.domain.workflow.node.base/g' \
    -e 's/import com\.zhouruojun\.manus\.core\.node\.specialized/import com.zhouruojun.manus.domain.workflow.node.specialized/g' \
    -e 's/import com\.zhouruojun\.manus\.core/import com.zhouruojun.manus.domain.workflow.engine/g' \
    -e 's/import com\.zhouruojun\.manus\.model/import com.zhouruojun.manus.domain.model/g' \
    -e 's/import com\.zhouruojun\.manus\.service/import com.zhouruojun.manus.application.service/g' \
    -e 's/import com\.zhouruojun\.manus\.config/import com.zhouruojun.manus.application.config/g' \
    -e 's/import com\.zhouruojun\.manus\.tools/import com.zhouruojun.manus.infrastructure.tools/g' \
    -e 's/import com\.zhouruojun\.manus\.serializers/import com.zhouruojun.manus.infrastructure.serializers/g' \
    -e 's/import com\.zhouruojun\.manus\.exception/import com.zhouruojun.manus.infrastructure.exception/g' \
    {} \; 