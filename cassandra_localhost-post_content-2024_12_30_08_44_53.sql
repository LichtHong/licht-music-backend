CREATE KEYSPACE IF NOT EXISTS licht_music WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

create table post_content
(
    id      uuid primary key,
    content text
);

INSERT INTO licht_music.post_content (id, content) VALUES (39d2210b-91cc-4853-bc75-83c9699f31c6, '图文帖子测试内容');
INSERT INTO licht_music.post_content (id, content) VALUES (6d1ce5c8-78f0-4883-83cd-bd6dd57ff9f6, '图文帖子测试内容：更新测试');
INSERT INTO licht_music.post_content (id, content) VALUES (a8725d83-34d9-4207-845a-67a4174b339d, '代码测试帖子内容插入');
