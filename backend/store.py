"""Key-value store: in memory locally, DynamoDB on AWS (partition key `pk`, TTL `expiresAt`)."""
from __future__ import annotations

import json
import threading
from typing import Optional


class MemoryStore:
    def __init__(self):
        self._items: dict = {}
        self._lock = threading.Lock()

    def get(self, key: str) -> Optional[dict]:
        with self._lock:
            item = self._items.get(key)
            return json.loads(item) if item else None

    def put(self, key: str, value: dict, expires_at: int) -> None:
        with self._lock:
            self._items[key] = json.dumps(value)

    def put_new(self, key: str, value: dict, expires_at: int) -> bool:
        """Atomic insert; False if the key already exists (used for one-time jti)."""
        with self._lock:
            if key in self._items:
                return False
            self._items[key] = json.dumps(value)
            return True


class DynamoStore:
    def __init__(self, table_name: str):
        import boto3  # provided by the Lambda runtime

        self._table = boto3.resource("dynamodb").Table(table_name)

    def get(self, key: str) -> Optional[dict]:
        item = self._table.get_item(Key={"pk": key}, ConsistentRead=True).get("Item")
        return json.loads(item["data"]) if item else None

    def put(self, key: str, value: dict, expires_at: int) -> None:
        self._table.put_item(Item={"pk": key, "data": json.dumps(value), "expiresAt": expires_at})

    def put_new(self, key: str, value: dict, expires_at: int) -> bool:
        from botocore.exceptions import ClientError

        try:
            self._table.put_item(
                Item={"pk": key, "data": json.dumps(value), "expiresAt": expires_at},
                ConditionExpression="attribute_not_exists(pk)",
            )
            return True
        except ClientError as error:
            if error.response["Error"]["Code"] == "ConditionalCheckFailedException":
                return False
            raise
