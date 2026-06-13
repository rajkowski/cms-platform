
ALTER TABLE web_pages ADD COLUMN IF NOT EXISTS page_text TEXT;
ALTER TABLE web_pages ADD COLUMN IF NOT EXISTS tsv TSVECTOR;

CREATE INDEX web_pages_tsv_idx ON web_pages USING gin(tsv);
-- CREATE INDEX web_pages_bm25_idx ON web_pages USING bm25 (page_text) WITH (text_config = 'english');

CREATE TEXT SEARCH DICTIONARY web_page_stem (
    TEMPLATE = snowball,
    Language = english
);
CREATE TEXT SEARCH CONFIGURATION web_page_stem (copy = english);
ALTER TEXT SEARCH CONFIGURATION web_page_stem
   ALTER MAPPING FOR asciihword, asciiword, hword, hword_asciipart, hword_part, word
   WITH web_page_stem;

CREATE OR REPLACE FUNCTION web_page_tsv_trigger() RETURNS trigger AS $$
begin
  new.tsv :=
    setweight(to_tsvector('web_page_stem', new.page_title), 'A') ||
    setweight(to_tsvector('web_page_stem', coalesce(new.page_keywords,'')), 'B') ||
    setweight(to_tsvector('web_page_stem', coalesce(new.page_description,'')), 'C') ||
    setweight(to_tsvector('web_page_stem', coalesce(new.page_text,'')), 'D');
  return new;
end
$$ LANGUAGE plpgsql;

CREATE TRIGGER tsvectorupdate BEFORE INSERT OR UPDATE
ON web_pages FOR EACH ROW EXECUTE PROCEDURE web_page_tsv_trigger();
