CREATE TABLE `plans` (
	`owner` text NOT NULL,
	`year` integer NOT NULL,
	`data` text NOT NULL,
	`updated_at` text NOT NULL,
	PRIMARY KEY(`owner`, `year`)
);
