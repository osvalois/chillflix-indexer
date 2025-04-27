-- DROP SCHEMA public;

CREATE SCHEMA public AUTHORIZATION postgres;

-- DROP SEQUENCE public.audit_log_id_seq;

CREATE SEQUENCE public.audit_log_id_seq
    INCREMENT BY 1
    MINVALUE 1
    MAXVALUE 2147483647
    START 1
	CACHE 1
	NO CYCLE;

-- Permissions

ALTER SEQUENCE public.audit_log_id_seq OWNER TO postgres;
GRANT ALL ON SEQUENCE public.audit_log_id_seq TO postgres;
-- public.audit_log definition

-- Drop table

-- DROP TABLE public.audit_log;

CREATE TABLE public.audit_log (
                                  id serial4 NOT NULL,
                                  entity_type varchar(20) NOT NULL,
                                  entity_id uuid NOT NULL,
                                  "action" varchar(10) NOT NULL,
                                  user_id varchar(50) NULL,
                                  changes jsonb NULL,
                                  "timestamp" timestamptz DEFAULT now() NOT NULL,
                                  "token" varchar(255) NULL,
                                  CONSTRAINT audit_log_pkey PRIMARY KEY (id)
);

-- Permissions

ALTER TABLE public.audit_log OWNER TO postgres;
GRANT ALL ON TABLE public.audit_log TO postgres;


-- public.flyway_schema_history definition

-- Drop table

-- DROP TABLE public.flyway_schema_history;

CREATE TABLE public.flyway_schema_history (
                                              installed_rank int4 NOT NULL,
                                              "version" varchar(50) NULL,
                                              description varchar(200) NOT NULL,
                                              "type" varchar(20) NOT NULL,
                                              script varchar(1000) NOT NULL,
                                              checksum int4 NULL,
                                              installed_by varchar(100) NOT NULL,
                                              installed_on timestamp DEFAULT now() NOT NULL,
                                              execution_time int4 NOT NULL,
                                              success bool NOT NULL,
                                              CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank)
);
CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);

-- Permissions

ALTER TABLE public.flyway_schema_history OWNER TO postgres;
GRANT ALL ON TABLE public.flyway_schema_history TO postgres;


-- public.import_jobs definition

-- Drop table

-- DROP TABLE public.import_jobs;

CREATE TABLE public.import_jobs (
                                    job_id uuid DEFAULT uuid_generate_v4() NOT NULL,
                                    status varchar(20) DEFAULT 'PENDING'::character varying NOT NULL,
                                    payload jsonb NOT NULL,
                                    priority int4 DEFAULT 5 NOT NULL,
                                    attempts int4 DEFAULT 0 NOT NULL,
                                    max_attempts int4 DEFAULT 3 NOT NULL,
                                    created_at timestamptz DEFAULT now() NOT NULL,
                                    started_at timestamptz NULL,
                                    completed_at timestamptz NULL,
                                    next_attempt_at timestamptz NULL,
                                    error jsonb NULL,
                                    "result" jsonb NULL,
                                    ws_client_id text NULL,
                                    lock_id text NULL,
                                    locked_until timestamptz NULL,
                                    media_type varchar(20) DEFAULT 'movie'::character varying NOT NULL,
                                    CONSTRAINT import_jobs_pkey PRIMARY KEY (job_id)
);
CREATE INDEX idx_import_jobs_media_type ON public.import_jobs USING btree (media_type);
CREATE INDEX idx_jobs_locked_until ON public.import_jobs USING btree (locked_until) WHERE ((status)::text = 'PROCESSING'::text);
CREATE INDEX idx_jobs_next_attempt ON public.import_jobs USING btree (next_attempt_at) WHERE ((status)::text = 'PENDING'::text);
CREATE INDEX idx_jobs_status ON public.import_jobs USING btree (status);

-- Table Triggers

create trigger import_jobs_audit after
    insert
    or
delete
or
update
    on
    public.import_jobs for each row execute function log_audit();

-- Permissions

ALTER TABLE public.import_jobs OWNER TO postgres;
GRANT ALL ON TABLE public.import_jobs TO postgres;


-- public.movies definition

-- Drop table

-- DROP TABLE public.movies;

CREATE TABLE public.movies (
                               id uuid DEFAULT uuid_generate_v4() NOT NULL,
                               title varchar(255) NOT NULL,
                               "year" int4 NOT NULL,
                               magnet text NOT NULL,
                               tmdb_id int4 NULL,
                               imdb_id varchar(20) NULL,
                               "language" varchar(50) NULL,
                               original_language varchar(50) NULL,
                               quality varchar(20) NULL,
                               file_type varchar(20) NULL,
                               sha256_hash bpchar(64) NULL,
                               is_deleted bool DEFAULT false NULL,
                               created_at timestamptz NOT NULL,
                               updated_at timestamptz NOT NULL,
                               search_vector tsvector NULL,
                               "size" int8 NULL,
                               seeds int4 NULL,
                               peers int4 NULL,
                               overview text NULL,
                               poster_path text NULL,
                               genres _varchar NULL,
                               torrent_url text NULL,
                               trailer_url text NULL,
                               CONSTRAINT movies_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_movies_imdb_id ON public.movies USING btree (imdb_id);
CREATE INDEX idx_movies_language ON public.movies USING btree (language);
CREATE UNIQUE INDEX idx_movies_sha256_hash ON public.movies USING btree (sha256_hash);
CREATE INDEX idx_movies_title ON public.movies USING btree (lower((title)::text));
CREATE INDEX idx_movies_tmdb_id ON public.movies USING btree (tmdb_id);
CREATE INDEX idx_movies_year ON public.movies USING btree (year);
CREATE INDEX movies_search_idx ON public.movies USING gin (search_vector);

-- Table Triggers

create trigger update_movies_modtime before
    update
    on
        public.movies for each row execute function update_modified_column();
create trigger movies_search_update before
    insert
    or
update
    on
    public.movies for each row execute function movies_search_trigger();
create trigger movies_audit after
    insert
    or
delete
or
update
    on
    public.movies for each row execute function log_audit();

-- Permissions

ALTER TABLE public.movies OWNER TO postgres;
GRANT ALL ON TABLE public.movies TO postgres;


-- public.music definition

-- Drop table

-- DROP TABLE public.music;

CREATE TABLE public.music (
                              id uuid DEFAULT uuid_generate_v4() NOT NULL,
                              title varchar(255) NOT NULL,
                              artist varchar(255) NOT NULL,
                              album varchar(255) NULL,
                              "year" int4 NULL,
                              genre varchar(100) NULL,
                              track_count int4 NULL,
                              magnet text NOT NULL,
                              quality varchar(50) NULL,
                              file_type varchar(20) NULL,
                              "size" int8 NULL,
                              sha256_hash bpchar(64) NULL,
                              seeds int4 NULL,
                              peers int4 NULL,
                              cover_path text NULL,
                              description text NULL,
                              "label" varchar(100) NULL,
                              release_date timestamptz NULL,
                              torrent_url text NULL,
                              is_deleted bool DEFAULT false NULL,
                              created_at timestamptz DEFAULT now() NOT NULL,
                              updated_at timestamptz DEFAULT now() NOT NULL,
                              search_vector tsvector NULL,
                              CONSTRAINT music_pkey PRIMARY KEY (id),
                              CONSTRAINT music_sha256_hash_key UNIQUE (sha256_hash)
);
CREATE INDEX idx_music_album ON public.music USING btree (lower((album)::text));
CREATE INDEX idx_music_artist ON public.music USING btree (lower((artist)::text));
CREATE INDEX idx_music_genre ON public.music USING btree (genre);
CREATE INDEX idx_music_search ON public.music USING gin (search_vector);
CREATE INDEX idx_music_title ON public.music USING btree (lower((title)::text));
CREATE INDEX idx_music_year ON public.music USING btree (year);

-- Table Triggers

create trigger update_music_modtime before
    update
    on
        public.music for each row execute function update_modified_column();
create trigger music_search_update before
    insert
    or
update
    on
    public.music for each row execute function music_search_trigger();

-- Permissions

ALTER TABLE public.music OWNER TO postgres;
GRANT ALL ON TABLE public.music TO postgres;


-- public.oauth_providers definition

-- Drop table

-- DROP TABLE public.oauth_providers;

CREATE TABLE public.oauth_providers (
                                        id uuid DEFAULT uuid_generate_v4() NOT NULL,
                                        "name" varchar(50) NOT NULL,
                                        created_at timestamp DEFAULT now() NOT NULL,
                                        CONSTRAINT oauth_providers_name_key UNIQUE (name),
                                        CONSTRAINT oauth_providers_pkey PRIMARY KEY (id)
);

-- Permissions

ALTER TABLE public.oauth_providers OWNER TO postgres;
GRANT ALL ON TABLE public.oauth_providers TO postgres;


-- public.permissions definition

-- Drop table

-- DROP TABLE public.permissions;

CREATE TABLE public.permissions (
                                    id uuid DEFAULT uuid_generate_v4() NOT NULL,
                                    "name" varchar(100) NOT NULL,
                                    description text NULL,
                                    resource varchar(50) NOT NULL,
                                    "action" varchar(50) NOT NULL,
                                    created_at timestamp DEFAULT now() NOT NULL,
                                    updated_at timestamp DEFAULT now() NOT NULL,
                                    CONSTRAINT permissions_name_key UNIQUE (name),
                                    CONSTRAINT permissions_pkey PRIMARY KEY (id),
                                    CONSTRAINT permissions_resource_action_key UNIQUE (resource, action)
);
CREATE INDEX idx_permissions_action ON public.permissions USING btree (action);
CREATE INDEX idx_permissions_name ON public.permissions USING btree (name);
CREATE INDEX idx_permissions_resource ON public.permissions USING btree (resource);

-- Table Triggers

create trigger update_permissions_modtime before
    update
    on
        public.permissions for each row execute function update_modified_column();
create trigger permissions_audit after
    insert
    or
delete
or
update
    on
    public.permissions for each row execute function log_audit();

-- Permissions

ALTER TABLE public.permissions OWNER TO postgres;
GRANT ALL ON TABLE public.permissions TO postgres;


-- public.roles definition

-- Drop table

-- DROP TABLE public.roles;

CREATE TABLE public.roles (
                              id uuid DEFAULT uuid_generate_v4() NOT NULL,
                              "name" varchar(50) NOT NULL,
                              description text NULL,
                              created_at timestamp DEFAULT now() NOT NULL,
                              updated_at timestamp DEFAULT now() NOT NULL,
                              CONSTRAINT roles_name_key UNIQUE (name),
                              CONSTRAINT roles_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_roles_name ON public.roles USING btree (name);

-- Table Triggers

create trigger update_roles_modtime before
    update
    on
        public.roles for each row execute function update_modified_column();
create trigger roles_audit after
    insert
    or
delete
or
update
    on
    public.roles for each row execute function log_audit();

-- Permissions

ALTER TABLE public.roles OWNER TO postgres;
GRANT ALL ON TABLE public.roles TO postgres;


-- public.scheduled_jobs definition

-- Drop table

-- DROP TABLE public.scheduled_jobs;

CREATE TABLE public.scheduled_jobs (
                                       schedule_id uuid DEFAULT uuid_generate_v4() NOT NULL,
                                       "name" text NOT NULL,
                                       cron_expression text NOT NULL,
                                       payload jsonb NOT NULL,
                                       enabled bool DEFAULT true NOT NULL,
                                       last_run_at timestamptz NULL,
                                       next_run_at timestamptz NULL,
                                       created_at timestamptz DEFAULT now() NOT NULL,
                                       media_type varchar(20) DEFAULT 'movie'::character varying NOT NULL,
                                       CONSTRAINT scheduled_jobs_pkey PRIMARY KEY (schedule_id)
);
CREATE INDEX idx_scheduled_jobs_enabled ON public.scheduled_jobs USING btree (enabled) WHERE (enabled = true);
CREATE INDEX idx_scheduled_jobs_media_type ON public.scheduled_jobs USING btree (media_type);

-- Table Triggers

create trigger scheduled_jobs_audit after
    insert
    or
delete
or
update
    on
    public.scheduled_jobs for each row execute function log_audit();

-- Permissions

ALTER TABLE public.scheduled_jobs OWNER TO postgres;
GRANT ALL ON TABLE public.scheduled_jobs TO postgres;


-- public.series definition

-- Drop table

-- DROP TABLE public.series;

CREATE TABLE public.series (
                               id uuid DEFAULT uuid_generate_v4() NOT NULL,
                               title varchar(255) NOT NULL,
                               "year" int4 NOT NULL,
                               magnet text NOT NULL,
                               tmdb_id int4 NULL,
                               imdb_id varchar(20) NULL,
                               "language" varchar(50) NULL,
                               original_language varchar(50) NULL,
                               quality varchar(20) NULL,
                               file_type varchar(20) NULL,
                               sha256_hash bpchar(64) NULL,
                               is_deleted bool DEFAULT false NULL,
                               created_at timestamptz DEFAULT now() NOT NULL,
                               updated_at timestamptz DEFAULT now() NOT NULL,
                               search_vector tsvector NULL,
                               "size" int8 NULL,
                               seeds int4 NULL,
                               peers int4 NULL,
                               overview text NULL,
                               poster_path text NULL,
                               genres _varchar NULL,
                               torrent_url text NULL,
                               trailer_url text NULL,
                               seasons int4 NULL,
                               episodes int4 NULL,
                               network varchar(100) NULL,
                               status varchar(50) NULL,
                               episode_runtime int4 NULL,
                               CONSTRAINT series_pkey PRIMARY KEY (id),
                               CONSTRAINT series_sha256_hash_key UNIQUE (sha256_hash)
);
CREATE INDEX idx_series_imdb_id ON public.series USING btree (imdb_id);
CREATE INDEX idx_series_language ON public.series USING btree (language);
CREATE INDEX idx_series_search ON public.series USING gin (search_vector);
CREATE INDEX idx_series_title ON public.series USING btree (lower((title)::text));
CREATE INDEX idx_series_tmdb_id ON public.series USING btree (tmdb_id);
CREATE INDEX idx_series_year ON public.series USING btree (year);

-- Table Triggers

create trigger update_series_modtime before
    update
    on
        public.series for each row execute function update_modified_column();
create trigger series_search_update before
    insert
    or
update
    on
    public.series for each row execute function series_search_trigger();

-- Permissions

ALTER TABLE public.series OWNER TO postgres;
GRANT ALL ON TABLE public.series TO postgres;


-- public.torrents definition

-- Drop table

-- DROP TABLE public.torrents;

CREATE TABLE public.torrents (
                                 info_hash varchar(40) NOT NULL,
                                 "name" varchar(255) NOT NULL,
                                 total_length int8 NOT NULL,
                                 created_at timestamptz NOT NULL,
                                 updated_at timestamptz NOT NULL,
                                 CONSTRAINT torrents_pkey PRIMARY KEY (info_hash)
);

-- Table Triggers

create trigger update_torrents_modtime before
    update
    on
        public.torrents for each row execute function update_modified_column();

-- Permissions

ALTER TABLE public.torrents OWNER TO postgres;
GRANT ALL ON TABLE public.torrents TO postgres;


-- public.users definition

-- Drop table

-- DROP TABLE public.users;

CREATE TABLE public.users (
                              id uuid DEFAULT uuid_generate_v4() NOT NULL,
                              username varchar(100) NOT NULL,
                              email varchar(255) NOT NULL,
                              full_name varchar(255) NULL,
                              hashed_password varchar(255) NULL,
                              "role" varchar(50) DEFAULT 'user'::character varying NOT NULL,
                              disabled bool DEFAULT false NOT NULL,
                              created_at timestamp DEFAULT now() NOT NULL,
                              updated_at timestamp DEFAULT now() NOT NULL,
                              last_login timestamp NULL,
                              api_key varchar(100) NULL,
                              preferences jsonb DEFAULT '{}'::jsonb NOT NULL,
                              email_verified bool DEFAULT false NOT NULL,
                              firebase_uid varchar(128) NULL,
                              CONSTRAINT users_api_key_key UNIQUE (api_key),
                              CONSTRAINT users_email_key UNIQUE (email),
                              CONSTRAINT users_firebase_uid_key UNIQUE (firebase_uid),
                              CONSTRAINT users_pkey PRIMARY KEY (id),
                              CONSTRAINT users_username_key UNIQUE (username)
);
CREATE INDEX idx_users_disabled ON public.users USING btree (disabled);
CREATE INDEX idx_users_email ON public.users USING btree (email);
CREATE INDEX idx_users_firebase_uid ON public.users USING btree (firebase_uid);
CREATE INDEX idx_users_username ON public.users USING btree (username);

-- Table Triggers

create trigger update_users_modtime before
    update
    on
        public.users for each row execute function update_modified_column();
create trigger users_audit after
    insert
    or
delete
or
update
    on
    public.users for each row execute function log_audit();

-- Permissions

ALTER TABLE public.users OWNER TO postgres;
GRANT ALL ON TABLE public.users TO postgres;


-- public.video_games definition

-- Drop table

-- DROP TABLE public.video_games;

CREATE TABLE public.video_games (
                                    id uuid DEFAULT uuid_generate_v4() NOT NULL,
                                    title varchar(255) NOT NULL,
                                    "year" int4 NULL,
                                    developer varchar(255) NULL,
                                    publisher varchar(255) NULL,
                                    platform _varchar NOT NULL,
                                    magnet text NOT NULL,
                                    quality varchar(50) NULL,
                                    file_type varchar(20) NULL,
                                    "size" int8 NULL,
                                    sha256_hash bpchar(64) NULL,
                                    seeds int4 NULL,
                                    peers int4 NULL,
                                    cover_path text NULL,
                                    description text NULL,
                                    system_requirements jsonb NULL,
                                    genre _varchar NULL,
                                    screenshot_paths _varchar NULL,
                                    rating varchar(10) NULL,
                                    release_date timestamptz NULL,
                                    torrent_url text NULL,
                                    esrb_rating varchar(10) NULL,
                                    multiplayer bool NULL,
                                    is_deleted bool DEFAULT false NULL,
                                    created_at timestamptz DEFAULT now() NOT NULL,
                                    updated_at timestamptz DEFAULT now() NOT NULL,
                                    search_vector tsvector NULL,
                                    CONSTRAINT video_games_pkey PRIMARY KEY (id),
                                    CONSTRAINT video_games_sha256_hash_key UNIQUE (sha256_hash)
);
CREATE INDEX idx_video_games_developer ON public.video_games USING btree (lower((developer)::text));
CREATE INDEX idx_video_games_publisher ON public.video_games USING btree (lower((publisher)::text));
CREATE INDEX idx_video_games_search ON public.video_games USING gin (search_vector);
CREATE INDEX idx_video_games_title ON public.video_games USING btree (lower((title)::text));
CREATE INDEX idx_video_games_year ON public.video_games USING btree (year);

-- Table Triggers

create trigger update_video_games_modtime before
    update
    on
        public.video_games for each row execute function update_modified_column();
create trigger video_games_search_update before
    insert
    or
update
    on
    public.video_games for each row execute function video_games_search_trigger();

-- Permissions

ALTER TABLE public.video_games OWNER TO postgres;
GRANT ALL ON TABLE public.video_games TO postgres;


-- public.videos definition

-- Drop table

-- DROP TABLE public.videos;

CREATE TABLE public.videos (
                               id uuid DEFAULT uuid_generate_v4() NOT NULL,
                               title varchar(255) NOT NULL,
                               creator varchar(255) NULL,
                               "year" int4 NULL,
                               duration int4 NULL,
                               category varchar(100) NULL,
                               magnet text NOT NULL,
                               quality varchar(50) NULL,
                               file_type varchar(20) NULL,
                               "size" int8 NULL,
                               sha256_hash bpchar(64) NULL,
                               seeds int4 NULL,
                               peers int4 NULL,
                               thumbnail_path text NULL,
                               description text NULL,
                               tags _varchar NULL,
                               torrent_url text NULL,
                               "source" varchar(100) NULL,
                               is_deleted bool DEFAULT false NULL,
                               created_at timestamptz DEFAULT now() NOT NULL,
                               updated_at timestamptz DEFAULT now() NOT NULL,
                               search_vector tsvector NULL,
                               CONSTRAINT videos_pkey PRIMARY KEY (id),
                               CONSTRAINT videos_sha256_hash_key UNIQUE (sha256_hash)
);
CREATE INDEX idx_videos_category ON public.videos USING btree (category);
CREATE INDEX idx_videos_creator ON public.videos USING btree (lower((creator)::text));
CREATE INDEX idx_videos_search ON public.videos USING gin (search_vector);
CREATE INDEX idx_videos_title ON public.videos USING btree (lower((title)::text));
CREATE INDEX idx_videos_year ON public.videos USING btree (year);

-- Table Triggers

create trigger update_videos_modtime before
    update
    on
        public.videos for each row execute function update_modified_column();
create trigger videos_search_update before
    insert
    or
update
    on
    public.videos for each row execute function videos_search_trigger();

-- Permissions

ALTER TABLE public.videos OWNER TO postgres;
GRANT ALL ON TABLE public.videos TO postgres;


-- public.email_verification_tokens definition

-- Drop table

-- DROP TABLE public.email_verification_tokens;

CREATE TABLE public.email_verification_tokens (
                                                  id uuid DEFAULT uuid_generate_v4() NOT NULL,
                                                  user_id uuid NOT NULL,
                                                  "token" varchar(255) NOT NULL,
                                                  expires_at timestamp NOT NULL,
                                                  created_at timestamp DEFAULT now() NOT NULL,
                                                  used bool DEFAULT false NOT NULL,
                                                  used_at timestamp NULL,
                                                  CONSTRAINT email_verification_tokens_pkey PRIMARY KEY (id),
                                                  CONSTRAINT email_verification_tokens_token_key UNIQUE (token),
                                                  CONSTRAINT email_verification_tokens_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE
);
CREATE INDEX idx_email_verification_tokens_expires_at ON public.email_verification_tokens USING btree (expires_at);
CREATE INDEX idx_email_verification_tokens_token ON public.email_verification_tokens USING btree (token);
CREATE INDEX idx_email_verification_tokens_user_id ON public.email_verification_tokens USING btree (user_id);

-- Table Triggers

create trigger email_verification_tokens_audit after
    insert
    or
delete
or
update
    on
    public.email_verification_tokens for each row execute function log_audit();

-- Permissions

ALTER TABLE public.email_verification_tokens OWNER TO postgres;
GRANT ALL ON TABLE public.email_verification_tokens TO postgres;


-- public.music_tracks definition

-- Drop table

-- DROP TABLE public.music_tracks;

CREATE TABLE public.music_tracks (
                                     id uuid DEFAULT uuid_generate_v4() NOT NULL,
                                     album_id uuid NOT NULL,
                                     track_number int4 NOT NULL,
                                     title varchar(255) NOT NULL,
                                     artist varchar(255) NULL,
                                     duration int4 NULL,
                                     file_path text NULL,
                                     file_type varchar(20) NULL,
                                     sha256_hash bpchar(64) NULL,
                                     created_at timestamptz DEFAULT now() NOT NULL,
                                     updated_at timestamptz DEFAULT now() NOT NULL,
                                     CONSTRAINT music_tracks_album_id_track_number_key UNIQUE (album_id, track_number),
                                     CONSTRAINT music_tracks_pkey PRIMARY KEY (id),
                                     CONSTRAINT music_tracks_album_id_fkey FOREIGN KEY (album_id) REFERENCES public.music(id) ON DELETE CASCADE
);

-- Table Triggers

create trigger update_music_tracks_modtime before
    update
    on
        public.music_tracks for each row execute function update_modified_column();
create trigger music_tracks_audit after
    insert
    or
delete
or
update
    on
    public.music_tracks for each row execute function log_audit();

-- Permissions

ALTER TABLE public.music_tracks OWNER TO postgres;
GRANT ALL ON TABLE public.music_tracks TO postgres;


-- public.password_reset_tokens definition

-- Drop table

-- DROP TABLE public.password_reset_tokens;

CREATE TABLE public.password_reset_tokens (
                                              id uuid DEFAULT uuid_generate_v4() NOT NULL,
                                              user_id uuid NOT NULL,
                                              "token" varchar(255) NOT NULL,
                                              expires_at timestamp NOT NULL,
                                              created_at timestamp DEFAULT now() NOT NULL,
                                              used bool DEFAULT false NOT NULL,
                                              used_at timestamp NULL,
                                              CONSTRAINT password_reset_tokens_pkey PRIMARY KEY (id),
                                              CONSTRAINT password_reset_tokens_token_key UNIQUE (token),
                                              CONSTRAINT password_reset_tokens_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE
);
CREATE INDEX idx_password_reset_tokens_expires_at ON public.password_reset_tokens USING btree (expires_at);
CREATE INDEX idx_password_reset_tokens_token ON public.password_reset_tokens USING btree (token);
CREATE INDEX idx_password_reset_tokens_user_id ON public.password_reset_tokens USING btree (user_id);

-- Table Triggers

create trigger password_reset_tokens_audit after
    insert
    or
delete
or
update
    on
    public.password_reset_tokens for each row execute function log_audit();

-- Permissions

ALTER TABLE public.password_reset_tokens OWNER TO postgres;
GRANT ALL ON TABLE public.password_reset_tokens TO postgres;


-- public.refresh_tokens definition

-- Drop table

-- DROP TABLE public.refresh_tokens;

CREATE TABLE public.refresh_tokens (
                                       id uuid DEFAULT uuid_generate_v4() NOT NULL,
                                       user_id uuid NOT NULL,
                                       "token" varchar(255) NOT NULL,
                                       expires_at timestamp NOT NULL,
                                       created_at timestamp DEFAULT now() NOT NULL,
                                       revoked bool DEFAULT false NOT NULL,
                                       revoked_at timestamp NULL,
                                       CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id),
                                       CONSTRAINT refresh_tokens_token_key UNIQUE (token),
                                       CONSTRAINT refresh_tokens_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE
);
CREATE INDEX idx_refresh_tokens_expires_at ON public.refresh_tokens USING btree (expires_at);
CREATE INDEX idx_refresh_tokens_token ON public.refresh_tokens USING btree (token);
CREATE INDEX idx_refresh_tokens_user_id ON public.refresh_tokens USING btree (user_id);

-- Table Triggers

create trigger refresh_tokens_audit after
    insert
    or
delete
or
update
    on
    public.refresh_tokens for each row execute function log_audit();

-- Permissions

ALTER TABLE public.refresh_tokens OWNER TO postgres;
GRANT ALL ON TABLE public.refresh_tokens TO postgres;


-- public.role_permissions definition

-- Drop table

-- DROP TABLE public.role_permissions;

CREATE TABLE public.role_permissions (
                                         role_id uuid NOT NULL,
                                         permission_id uuid NOT NULL,
                                         created_at timestamp DEFAULT now() NOT NULL,
                                         CONSTRAINT role_permissions_pkey PRIMARY KEY (role_id, permission_id),
                                         CONSTRAINT role_permissions_permission_id_fkey FOREIGN KEY (permission_id) REFERENCES public.permissions(id) ON DELETE CASCADE,
                                         CONSTRAINT role_permissions_role_id_fkey FOREIGN KEY (role_id) REFERENCES public.roles(id) ON DELETE CASCADE
);
CREATE INDEX idx_role_permissions_permission_id ON public.role_permissions USING btree (permission_id);
CREATE INDEX idx_role_permissions_role_id ON public.role_permissions USING btree (role_id);

-- Table Triggers

create trigger role_permissions_audit after
    insert
    or
delete
or
update
    on
    public.role_permissions for each row execute function log_audit_junction();

-- Permissions

ALTER TABLE public.role_permissions OWNER TO postgres;
GRANT ALL ON TABLE public.role_permissions TO postgres;


-- public.series_episodes definition

-- Drop table

-- DROP TABLE public.series_episodes;

CREATE TABLE public.series_episodes (
                                        id uuid DEFAULT uuid_generate_v4() NOT NULL,
                                        series_id uuid NOT NULL,
                                        season_number int4 NOT NULL,
                                        episode_number int4 NOT NULL,
                                        title varchar(255) NULL,
                                        overview text NULL,
                                        air_date timestamptz NULL,
                                        runtime int4 NULL,
                                        magnet text NULL,
                                        quality varchar(20) NULL,
                                        "size" int8 NULL,
                                        file_type varchar(20) NULL,
                                        sha256_hash bpchar(64) NULL,
                                        created_at timestamptz DEFAULT now() NOT NULL,
                                        updated_at timestamptz DEFAULT now() NOT NULL,
                                        CONSTRAINT series_episodes_pkey PRIMARY KEY (id),
                                        CONSTRAINT series_episodes_series_id_season_number_episode_number_key UNIQUE (series_id, season_number, episode_number),
                                        CONSTRAINT series_episodes_series_id_fkey FOREIGN KEY (series_id) REFERENCES public.series(id) ON DELETE CASCADE
);

-- Table Triggers

create trigger update_series_episodes_modtime before
    update
    on
        public.series_episodes for each row execute function update_modified_column();
create trigger series_episodes_audit after
    insert
    or
delete
or
update
    on
    public.series_episodes for each row execute function log_audit();

-- Permissions

ALTER TABLE public.series_episodes OWNER TO postgres;
GRANT ALL ON TABLE public.series_episodes TO postgres;


-- public.torrent_files definition

-- Drop table

-- DROP TABLE public.torrent_files;

CREATE TABLE public.torrent_files (
                                      torrent_info_hash varchar(40) NOT NULL,
                                      file_index int4 NOT NULL,
                                      "path" text NOT NULL,
                                      length int8 NOT NULL,
                                      CONSTRAINT torrent_files_pkey PRIMARY KEY (torrent_info_hash, file_index),
                                      CONSTRAINT torrent_files_torrent_info_hash_fkey FOREIGN KEY (torrent_info_hash) REFERENCES public.torrents(info_hash) ON DELETE CASCADE
);

-- Permissions

ALTER TABLE public.torrent_files OWNER TO postgres;
GRANT ALL ON TABLE public.torrent_files TO postgres;


-- public.user_oauth_providers definition

-- Drop table

-- DROP TABLE public.user_oauth_providers;

CREATE TABLE public.user_oauth_providers (
                                             user_id uuid NOT NULL,
                                             provider_id uuid NOT NULL,
                                             provider_user_id varchar(255) NOT NULL,
                                             access_token text NULL,
                                             refresh_token text NULL,
                                             expires_at timestamp NULL,
                                             created_at timestamp DEFAULT now() NOT NULL,
                                             updated_at timestamp DEFAULT now() NOT NULL,
                                             CONSTRAINT user_oauth_providers_pkey PRIMARY KEY (user_id, provider_id),
                                             CONSTRAINT user_oauth_providers_provider_id_fkey FOREIGN KEY (provider_id) REFERENCES public.oauth_providers(id),
                                             CONSTRAINT user_oauth_providers_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id)
);

-- Permissions

ALTER TABLE public.user_oauth_providers OWNER TO postgres;
GRANT ALL ON TABLE public.user_oauth_providers TO postgres;


-- public.user_roles definition

-- Drop table

-- DROP TABLE public.user_roles;

CREATE TABLE public.user_roles (
                                   user_id uuid NOT NULL,
                                   role_id uuid NOT NULL,
                                   created_at timestamp DEFAULT now() NOT NULL,
                                   CONSTRAINT user_roles_pkey PRIMARY KEY (user_id, role_id),
                                   CONSTRAINT user_roles_role_id_fkey FOREIGN KEY (role_id) REFERENCES public.roles(id) ON DELETE CASCADE,
                                   CONSTRAINT user_roles_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE
);
CREATE INDEX idx_user_roles_role_id ON public.user_roles USING btree (role_id);
CREATE INDEX idx_user_roles_user_id ON public.user_roles USING btree (user_id);

-- Table Triggers

create trigger user_roles_audit after
    insert
    or
delete
or
update
    on
    public.user_roles for each row execute function log_audit_junction();

-- Permissions

ALTER TABLE public.user_roles OWNER TO postgres;
GRANT ALL ON TABLE public.user_roles TO postgres;


-- public.user_sessions definition

-- Drop table

-- DROP TABLE public.user_sessions;

CREATE TABLE public.user_sessions (
                                      id uuid DEFAULT uuid_generate_v4() NOT NULL,
                                      user_id uuid NOT NULL,
                                      "token" varchar(255) NOT NULL,
                                      ip_address varchar(50) NULL,
                                      user_agent text NULL,
                                      created_at timestamp DEFAULT now() NOT NULL,
                                      expires_at timestamp NOT NULL,
                                      is_valid bool DEFAULT true NOT NULL,
                                      last_activity timestamp DEFAULT now() NOT NULL,
                                      device_id varchar(100) NULL,
                                      device_type varchar(50) NULL,
                                      device_name varchar(100) NULL,
                                      "location" varchar(200) NULL,
                                      CONSTRAINT user_sessions_pkey PRIMARY KEY (id),
                                      CONSTRAINT user_sessions_token_key UNIQUE (token),
                                      CONSTRAINT user_sessions_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id)
);
CREATE INDEX idx_user_sessions_device_id ON public.user_sessions USING btree (device_id);

-- Permissions

ALTER TABLE public.user_sessions OWNER TO postgres;
GRANT ALL ON TABLE public.user_sessions TO postgres;


-- public.pg_stat_statements source

CREATE OR REPLACE VIEW public.pg_stat_statements
AS SELECT userid,
          dbid,
          toplevel,
          queryid,
          query,
          plans,
          total_plan_time,
          min_plan_time,
          max_plan_time,
          mean_plan_time,
          stddev_plan_time,
          calls,
          total_exec_time,
          min_exec_time,
          max_exec_time,
          mean_exec_time,
          stddev_exec_time,
                        rows,
                        shared_blks_hit,
                        shared_blks_read,
                        shared_blks_dirtied,
                        shared_blks_written,
                        local_blks_hit,
                        local_blks_read,
                        local_blks_dirtied,
                        local_blks_written,
                        temp_blks_read,
                        temp_blks_written,
                        blk_read_time,
                        blk_write_time,
                        temp_blk_read_time,
                        temp_blk_write_time,
                        wal_records,
                        wal_fpi,
                        wal_bytes,
                        jit_functions,
                        jit_generation_time,
                        jit_inlining_count,
                        jit_inlining_time,
                        jit_optimization_count,
                        jit_optimization_time,
                        jit_emission_count,
                        jit_emission_time
   FROM pg_stat_statements(true) pg_stat_statements(userid, dbid, toplevel, queryid, query, plans, total_plan_time, min_plan_time, max_plan_time, mean_plan_time, stddev_plan_time, calls, total_exec_time, min_exec_time, max_exec_time, mean_exec_time, stddev_exec_time, rows, shared_blks_hit, shared_blks_read, shared_blks_dirtied, shared_blks_written, local_blks_hit, local_blks_read, local_blks_dirtied, local_blks_written, temp_blks_read, temp_blks_written, blk_read_time, blk_write_time, temp_blk_read_time, temp_blk_write_time, wal_records, wal_fpi, wal_bytes, jit_functions, jit_generation_time, jit_inlining_count, jit_inlining_time, jit_optimization_count, jit_optimization_time, jit_emission_count, jit_emission_time);

-- Permissions

ALTER TABLE public.pg_stat_statements OWNER TO postgres;
GRANT ALL ON TABLE public.pg_stat_statements TO postgres;
GRANT SELECT ON TABLE public.pg_stat_statements TO public;


-- public.pg_stat_statements_info source

CREATE OR REPLACE VIEW public.pg_stat_statements_info
AS SELECT dealloc,
          stats_reset
   FROM pg_stat_statements_info() pg_stat_statements_info(dealloc, stats_reset);

-- Permissions

ALTER TABLE public.pg_stat_statements_info OWNER TO postgres;
GRANT ALL ON TABLE public.pg_stat_statements_info TO postgres;
GRANT SELECT ON TABLE public.pg_stat_statements_info TO public;



-- DROP FUNCTION public.get_nil_uuid();

CREATE OR REPLACE FUNCTION public.get_nil_uuid()
 RETURNS uuid
 LANGUAGE plpgsql
AS $function$
BEGIN
RETURN '00000000-0000-0000-0000-000000000000'::uuid;
END;
$function$
;

-- Permissions

ALTER FUNCTION public.get_nil_uuid() OWNER TO postgres;
GRANT ALL ON FUNCTION public.get_nil_uuid() TO postgres;

-- DROP FUNCTION public.log_audit();

CREATE OR REPLACE FUNCTION public.log_audit()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
DECLARE
entity_id_val uuid;
BEGIN
    -- Determinar el ID de la entidad según la tabla
    -- Para 'import_jobs' usamos job_id, para 'scheduled_jobs' usamos schedule_id, para el resto usamos id
    IF TG_TABLE_NAME = 'import_jobs' THEN
        IF (TG_OP = 'DELETE') THEN
            entity_id_val := OLD.job_id;
ELSE
            entity_id_val := NEW.job_id;
END IF;
    ELSIF TG_TABLE_NAME = 'scheduled_jobs' THEN
        IF (TG_OP = 'DELETE') THEN
            entity_id_val := OLD.schedule_id;
ELSE
            entity_id_val := NEW.schedule_id;
END IF;
ELSE
        -- Para el resto de tablas, asumimos que el ID está en la columna 'id'
        IF (TG_OP = 'DELETE') THEN
            entity_id_val := OLD.id;
ELSE
            entity_id_val := NEW.id;
END IF;
END IF;

    -- Insertar en la tabla de auditoría con el ID correspondiente
    IF (TG_OP = 'DELETE') THEN
        INSERT INTO audit_log (entity_type, entity_id, action, changes)
        VALUES (TG_TABLE_NAME, entity_id_val, 'DELETE', row_to_json(OLD));
    ELSIF (TG_OP = 'UPDATE') THEN
        INSERT INTO audit_log (entity_type, entity_id, action, changes)
        VALUES (TG_TABLE_NAME, entity_id_val, 'UPDATE',
            jsonb_build_object(
                'old', row_to_json(OLD),
                'new', row_to_json(NEW)
            ));
    ELSIF (TG_OP = 'INSERT') THEN
        INSERT INTO audit_log (entity_type, entity_id, action, changes)
        VALUES (TG_TABLE_NAME, entity_id_val, 'INSERT', row_to_json(NEW));
END IF;

RETURN NULL;
END;
$function$
;

-- Permissions

ALTER FUNCTION public.log_audit() OWNER TO postgres;
GRANT ALL ON FUNCTION public.log_audit() TO postgres;

-- DROP FUNCTION public.log_audit_junction();

CREATE OR REPLACE FUNCTION public.log_audit_junction()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
BEGIN
    IF (TG_OP = 'DELETE') THEN
        INSERT INTO audit_log (entity_type, entity_id, action, changes)
        VALUES (TG_TABLE_NAME, get_nil_uuid(), 'DELETE', row_to_json(OLD));
    ELSIF (TG_OP = 'UPDATE') THEN
        INSERT INTO audit_log (entity_type, entity_id, action, changes)
        VALUES (TG_TABLE_NAME, get_nil_uuid(), 'UPDATE',
            jsonb_build_object(
                'old', row_to_json(OLD),
                'new', row_to_json(NEW)
            ));
    ELSIF (TG_OP = 'INSERT') THEN
        INSERT INTO audit_log (entity_type, entity_id, action, changes)
        VALUES (TG_TABLE_NAME, get_nil_uuid(), 'INSERT', row_to_json(NEW));
END IF;
RETURN NULL;
END;
$function$
;

-- Permissions

ALTER FUNCTION public.log_audit_junction() OWNER TO postgres;
GRANT ALL ON FUNCTION public.log_audit_junction() TO postgres;

-- DROP FUNCTION public.movies_search_trigger();

CREATE OR REPLACE FUNCTION public.movies_search_trigger()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
BEGIN
    NEW.search_vector =
        setweight(to_tsvector('english', COALESCE(NEW.title, '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(NEW.overview, '')), 'B');
RETURN NEW;
END
$function$
;

-- Permissions

ALTER FUNCTION public.movies_search_trigger() OWNER TO postgres;
GRANT ALL ON FUNCTION public.movies_search_trigger() TO postgres;

-- DROP FUNCTION public.music_search_trigger();

CREATE OR REPLACE FUNCTION public.music_search_trigger()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
BEGIN
    NEW.search_vector =
        setweight(to_tsvector('english', COALESCE(NEW.title, '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(NEW.artist, '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(NEW.album, '')), 'B');
RETURN NEW;
END
$function$
;

-- Permissions

ALTER FUNCTION public.music_search_trigger() OWNER TO postgres;
GRANT ALL ON FUNCTION public.music_search_trigger() TO postgres;

-- DROP FUNCTION public.pg_stat_statements(in bool, out oid, out oid, out bool, out int8, out text, out int8, out float8, out float8, out float8, out float8, out float8, out int8, out float8, out float8, out float8, out float8, out float8, out int8, out int8, out int8, out int8, out int8, out int8, out int8, out int8, out int8, out int8, out int8, out float8, out float8, out float8, out float8, out int8, out int8, out numeric, out int8, out float8, out int8, out float8, out int8, out float8, out int8, out float8);

CREATE OR REPLACE FUNCTION public.pg_stat_statements(showtext boolean, OUT userid oid, OUT dbid oid, OUT toplevel boolean, OUT queryid bigint, OUT query text, OUT plans bigint, OUT total_plan_time double precision, OUT min_plan_time double precision, OUT max_plan_time double precision, OUT mean_plan_time double precision, OUT stddev_plan_time double precision, OUT calls bigint, OUT total_exec_time double precision, OUT min_exec_time double precision, OUT max_exec_time double precision, OUT mean_exec_time double precision, OUT stddev_exec_time double precision, OUT rows bigint, OUT shared_blks_hit bigint, OUT shared_blks_read bigint, OUT shared_blks_dirtied bigint, OUT shared_blks_written bigint, OUT local_blks_hit bigint, OUT local_blks_read bigint, OUT local_blks_dirtied bigint, OUT local_blks_written bigint, OUT temp_blks_read bigint, OUT temp_blks_written bigint, OUT blk_read_time double precision, OUT blk_write_time double precision, OUT temp_blk_read_time double precision, OUT temp_blk_write_time double precision, OUT wal_records bigint, OUT wal_fpi bigint, OUT wal_bytes numeric, OUT jit_functions bigint, OUT jit_generation_time double precision, OUT jit_inlining_count bigint, OUT jit_inlining_time double precision, OUT jit_optimization_count bigint, OUT jit_optimization_time double precision, OUT jit_emission_count bigint, OUT jit_emission_time double precision)
 RETURNS SETOF record
 LANGUAGE c
 PARALLEL SAFE STRICT
AS '$libdir/pg_stat_statements', $function$pg_stat_statements_1_10$function$
;

-- Permissions

ALTER FUNCTION public.pg_stat_statements(in bool, out oid, out oid, out bool, out int8, out text, out int8, out float8, out float8, out float8, out float8, out float8, out int8, out float8, out float8, out float8, out float8, out float8, out int8, out int8, out int8, out int8, out int8, out int8, out int8, out int8, out int8, out int8, out int8, out float8, out float8, out float8, out float8, out int8, out int8, out numeric, out int8, out float8, out int8, out float8, out int8, out float8, out int8, out float8) OWNER TO postgres;
GRANT ALL ON FUNCTION public.pg_stat_statements(in bool, out oid, out oid, out bool, out int8, out text, out int8, out float8, out float8, out float8, out float8, out float8, out int8, out float8, out float8, out float8, out float8, out float8, out int8, out int8, out int8, out int8, out int8, out int8, out int8, out int8, out int8, out int8, out int8, out float8, out float8, out float8, out float8, out int8, out int8, out numeric, out int8, out float8, out int8, out float8, out int8, out float8, out int8, out float8) TO postgres;

-- DROP FUNCTION public.pg_stat_statements_info(out int8, out timestamptz);

CREATE OR REPLACE FUNCTION public.pg_stat_statements_info(OUT dealloc bigint, OUT stats_reset timestamp with time zone)
 RETURNS record
 LANGUAGE c
 PARALLEL SAFE STRICT
AS '$libdir/pg_stat_statements', $function$pg_stat_statements_info$function$
;

-- Permissions

ALTER FUNCTION public.pg_stat_statements_info(out int8, out timestamptz) OWNER TO postgres;
GRANT ALL ON FUNCTION public.pg_stat_statements_info(out int8, out timestamptz) TO postgres;

-- DROP FUNCTION public.pg_stat_statements_reset(oid, oid, int8);

CREATE OR REPLACE FUNCTION public.pg_stat_statements_reset(userid oid DEFAULT 0, dbid oid DEFAULT 0, queryid bigint DEFAULT 0)
 RETURNS void
 LANGUAGE c
 PARALLEL SAFE STRICT
AS '$libdir/pg_stat_statements', $function$pg_stat_statements_reset_1_7$function$
;

-- Permissions

ALTER FUNCTION public.pg_stat_statements_reset(oid, oid, int8) OWNER TO postgres;
GRANT ALL ON FUNCTION public.pg_stat_statements_reset(oid, oid, int8) TO postgres;

-- DROP FUNCTION public.series_search_trigger();

CREATE OR REPLACE FUNCTION public.series_search_trigger()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
BEGIN
    NEW.search_vector =
        setweight(to_tsvector('english', COALESCE(NEW.title, '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(NEW.network, '')), 'B') ||
        setweight(to_tsvector('english', COALESCE(NEW.overview, '')), 'B');
RETURN NEW;
END
$function$
;

-- Permissions

ALTER FUNCTION public.series_search_trigger() OWNER TO postgres;
GRANT ALL ON FUNCTION public.series_search_trigger() TO postgres;

-- DROP FUNCTION public.update_modified_column();

CREATE OR REPLACE FUNCTION public.update_modified_column()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
BEGIN
    NEW.updated_at = NOW();
RETURN NEW;
END;
$function$
;

-- Permissions

ALTER FUNCTION public.update_modified_column() OWNER TO postgres;
GRANT ALL ON FUNCTION public.update_modified_column() TO postgres;

-- DROP FUNCTION public.uuid_generate_v1();

CREATE OR REPLACE FUNCTION public.uuid_generate_v1()
 RETURNS uuid
 LANGUAGE c
 PARALLEL SAFE STRICT
AS '$libdir/uuid-ossp', $function$uuid_generate_v1$function$
;

-- Permissions

ALTER FUNCTION public.uuid_generate_v1() OWNER TO postgres;
GRANT ALL ON FUNCTION public.uuid_generate_v1() TO postgres;

-- DROP FUNCTION public.uuid_generate_v1mc();

CREATE OR REPLACE FUNCTION public.uuid_generate_v1mc()
 RETURNS uuid
 LANGUAGE c
 PARALLEL SAFE STRICT
AS '$libdir/uuid-ossp', $function$uuid_generate_v1mc$function$
;

-- Permissions

ALTER FUNCTION public.uuid_generate_v1mc() OWNER TO postgres;
GRANT ALL ON FUNCTION public.uuid_generate_v1mc() TO postgres;

-- DROP FUNCTION public.uuid_generate_v3(uuid, text);

CREATE OR REPLACE FUNCTION public.uuid_generate_v3(namespace uuid, name text)
 RETURNS uuid
 LANGUAGE c
 IMMUTABLE PARALLEL SAFE STRICT
AS '$libdir/uuid-ossp', $function$uuid_generate_v3$function$
;

-- Permissions

ALTER FUNCTION public.uuid_generate_v3(uuid, text) OWNER TO postgres;
GRANT ALL ON FUNCTION public.uuid_generate_v3(uuid, text) TO postgres;

-- DROP FUNCTION public.uuid_generate_v4();

CREATE OR REPLACE FUNCTION public.uuid_generate_v4()
 RETURNS uuid
 LANGUAGE c
 PARALLEL SAFE STRICT
AS '$libdir/uuid-ossp', $function$uuid_generate_v4$function$
;

-- Permissions

ALTER FUNCTION public.uuid_generate_v4() OWNER TO postgres;
GRANT ALL ON FUNCTION public.uuid_generate_v4() TO postgres;

-- DROP FUNCTION public.uuid_generate_v5(uuid, text);

CREATE OR REPLACE FUNCTION public.uuid_generate_v5(namespace uuid, name text)
 RETURNS uuid
 LANGUAGE c
 IMMUTABLE PARALLEL SAFE STRICT
AS '$libdir/uuid-ossp', $function$uuid_generate_v5$function$
;

-- Permissions

ALTER FUNCTION public.uuid_generate_v5(uuid, text) OWNER TO postgres;
GRANT ALL ON FUNCTION public.uuid_generate_v5(uuid, text) TO postgres;

-- DROP FUNCTION public.uuid_nil();

CREATE OR REPLACE FUNCTION public.uuid_nil()
 RETURNS uuid
 LANGUAGE c
 IMMUTABLE PARALLEL SAFE STRICT
AS '$libdir/uuid-ossp', $function$uuid_nil$function$
;

-- Permissions

ALTER FUNCTION public.uuid_nil() OWNER TO postgres;
GRANT ALL ON FUNCTION public.uuid_nil() TO postgres;

-- DROP FUNCTION public.uuid_ns_dns();

CREATE OR REPLACE FUNCTION public.uuid_ns_dns()
 RETURNS uuid
 LANGUAGE c
 IMMUTABLE PARALLEL SAFE STRICT
AS '$libdir/uuid-ossp', $function$uuid_ns_dns$function$
;

-- Permissions

ALTER FUNCTION public.uuid_ns_dns() OWNER TO postgres;
GRANT ALL ON FUNCTION public.uuid_ns_dns() TO postgres;

-- DROP FUNCTION public.uuid_ns_oid();

CREATE OR REPLACE FUNCTION public.uuid_ns_oid()
 RETURNS uuid
 LANGUAGE c
 IMMUTABLE PARALLEL SAFE STRICT
AS '$libdir/uuid-ossp', $function$uuid_ns_oid$function$
;

-- Permissions

ALTER FUNCTION public.uuid_ns_oid() OWNER TO postgres;
GRANT ALL ON FUNCTION public.uuid_ns_oid() TO postgres;

-- DROP FUNCTION public.uuid_ns_url();

CREATE OR REPLACE FUNCTION public.uuid_ns_url()
 RETURNS uuid
 LANGUAGE c
 IMMUTABLE PARALLEL SAFE STRICT
AS '$libdir/uuid-ossp', $function$uuid_ns_url$function$
;

-- Permissions

ALTER FUNCTION public.uuid_ns_url() OWNER TO postgres;
GRANT ALL ON FUNCTION public.uuid_ns_url() TO postgres;

-- DROP FUNCTION public.uuid_ns_x500();

CREATE OR REPLACE FUNCTION public.uuid_ns_x500()
 RETURNS uuid
 LANGUAGE c
 IMMUTABLE PARALLEL SAFE STRICT
AS '$libdir/uuid-ossp', $function$uuid_ns_x500$function$
;

-- Permissions

ALTER FUNCTION public.uuid_ns_x500() OWNER TO postgres;
GRANT ALL ON FUNCTION public.uuid_ns_x500() TO postgres;

-- DROP FUNCTION public.video_games_search_trigger();

CREATE OR REPLACE FUNCTION public.video_games_search_trigger()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
BEGIN
    NEW.search_vector =
        setweight(to_tsvector('english', COALESCE(NEW.title, '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(NEW.developer, '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(NEW.publisher, '')), 'B') ||
        setweight(to_tsvector('english', COALESCE(NEW.description, '')), 'B');
RETURN NEW;
END
$function$
;

-- Permissions

ALTER FUNCTION public.video_games_search_trigger() OWNER TO postgres;
GRANT ALL ON FUNCTION public.video_games_search_trigger() TO postgres;

-- DROP FUNCTION public.videos_search_trigger();

CREATE OR REPLACE FUNCTION public.videos_search_trigger()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
BEGIN
    NEW.search_vector =
        setweight(to_tsvector('english', COALESCE(NEW.title, '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(NEW.creator, '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(NEW.description, '')), 'B');
RETURN NEW;
END
$function$
;

-- Permissions

ALTER FUNCTION public.videos_search_trigger() OWNER TO postgres;
GRANT ALL ON FUNCTION public.videos_search_trigger() TO postgres;


-- Permissions

GRANT ALL ON SCHEMA public TO postgres;