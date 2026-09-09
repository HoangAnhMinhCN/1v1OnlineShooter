create table player(
	id varchar(255) primary key,
    name varchar(255),
    password varchar(255)
);
create table chat_message(
	mess_id int auto_increment primary key,
    sender_id varchar(255),
    receiver_id varchar(255),
    content text,
    created_at datetime,
    foreign key (sender_id) references player(id),
    foreign key (receiver_id) references player(id)
);

