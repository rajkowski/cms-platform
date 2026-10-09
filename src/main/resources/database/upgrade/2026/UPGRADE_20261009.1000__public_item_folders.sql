-- Item folders auto-created when uploading files should have guests allowed and a default guest privacy type
UPDATE item_folders
SET allows_guests = true, guest_privacy_type = 3000
WHERE guest_privacy_type = -1;
