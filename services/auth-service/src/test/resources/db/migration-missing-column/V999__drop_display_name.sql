-- テストだけで使う。本物のマイグレーションの後に、users の display_name の列を消し、
-- Entity とテーブルが合わないときに起動が失敗することを確かめる(AC-DB-03)。
ALTER TABLE users DROP COLUMN display_name;
