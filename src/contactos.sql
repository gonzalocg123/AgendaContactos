DROP TABLE IF EXISTS CONTACTOS;

CREATE TABLE CONTACTOS (
                           Nombre TEXT NOT NULL,
                           Telefono TEXT NOT NULL,
                           WebPersonal TEXT,
                           Correo TEXT,
                           Imagen TEXT,
                           PRIMARY KEY (Nombre, Telefono)
);

INSERT INTO CONTACTOS VALUES(
                                'Gonzalo Chica Godino',
                                '123456789',
                                'https://www.linkwww.linkedin.com/in/gonzalo-chica-godino-27710a33a',
                                'gonchicagodi@gmail.com',
                                'imagenes/images.png'
                            );
