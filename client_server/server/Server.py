# -*- coding: utf-8 -*-
import socket
import random
import sys
import os

ENCODING = "ascii"
NEWLINE = "\r\n"
USERNAME = "bilkentstu"
PASS = "cs421f2019"

SERVER_SHUTDOWN_MESSAGE = "Server shutdown. Please fix your code according to the response message and retry."
Bear =  ["bear1.jpg",
         "bear2.jpg",
         "bear3.jpg"]

Cat =   ["cat1.jpg",
         "cat2.jpg",
         "cat3.jpg"]

Dog =   ["dog1.jpg",
         "dog2.jpg",
         "dog3.jpg"]

Shark = ["shark1.jpg",
         "shark2.jpg",
         "shark3.jpg"]

Labels = [Bear, Cat, Dog, Shark]


HEADER_SIZE = 2
MAX_DATA_SIZE = 2**(HEADER_SIZE*8) - 1

# Socket stuff
IP = sys.argv[1]
CONTROL_PORT = int(sys.argv[2])

class WrongLabelingException(Exception):
    pass

class NoSuchLabelException(Exception):
    pass

class ServerShutdownException(Exception):
    pass
       

def send_response(s, success, info=""):
    response = "OK" if success else "INVALID " + info
    response = response + "\r\n"
    s.sendall(response.encode())

def send_iget_response(s, success):

    code = "ISND"
    image, label_i = select_random_image()

    myfile = open(os.path.join("images", image), 'rb')
    data = myfile.read()
    size = len(data)
    bytesize = size.to_bytes(3, byteorder="big")

    response = code.encode()+bytesize+data
    s.sendall(response)

    return label_i

def receive_command(f):
    line = f.readline()[:-len(NEWLINE)]
    idx = line.find(" ")
    
    if idx == -1:
        idx = len(line)
    
    cmd = line[:idx]
    args = line[idx+1:]
    print("Command received:", cmd, args)
    return cmd, args

def shutdown():
    print(SERVER_SHUTDOWN_MESSAGE)
    raise ServerShutdownException

def auth_check(f, conn):
    
    # Username check
    check = False
    cmd, args = receive_command(f)
    
    if cmd == "USER":
        if args == USERNAME:
            send_response(conn, success=True)
            check = True
        else:
            send_response(conn, success=False, info="Wrong username.")
    else:
        send_response(conn, success=False, info="Wrong command. Expecting USER.")
        
    if not check:
        return check
        
    # Password check
    check = False
    cmd, args = receive_command(f)
    if cmd == "PASS":
        if args == PASS:
            send_response(conn, success=True)
            check = True
        else:
            send_response(conn, success=False, info="Wrong password.")
    else:
        send_response(conn, success=False, info="Wrong command. Expecting PASS.")
    
    if not check:
        return check
        

def select_random_image():
    label_index = random.randint(0, 3)
    sel_index = random.randint(0, 2)

    label = Labels[label_index]
    sel_image = label[sel_index]
    return sel_image, label_index

def label_check(args, labellist):
    retrived_labellist = [0,0,0]
    retrived_labellist = args.split(",")
    retrived_labellist = label_enumerator(retrived_labellist)
    wrong_labels = [True, True, True]

    i = 0
    while i<3:
        if labellist[i] != retrived_labellist[i]:
            wrong_labels[i] = False
        i += 1
    return wrong_labels

def label_enumerator(labellist):
    length = len(labellist)
    for i in range(length):
        if labellist[i] == 'bear':
            labellist[i] = 0
        elif labellist[i] == 'cat':
            labellist[i] = 1
        elif labellist[i] == 'dog':
            labellist[i] = 2
        elif labellist[i] == 'shark':
            labellist[i] = 3
        else:
            labellist[i] = 4
            raise NoSuchLabelException
    return labellist

# =============================================================================
# MAIN
# =============================================================================
if __name__ == "__main__":

    labellist = [0,0,0]

    # Create socket
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    

    try:
        # Listen from the control port
        s.bind((IP, CONTROL_PORT))
        s.listen(1)
        conn, addr = s.accept()
        print("Client connected.")
        
        # Readfile
        f = conn.makefile(buffering=1, encoding=ENCODING, newline=NEWLINE)
        
        # Authenticate and get client data port
        check = auth_check(f, conn)
        if check == False:
            shutdown()
            
        # Main loop
        while True:
            cmd, args = receive_command(f)
            
            if cmd == "IGET":               
                # Send IGET response
                i = 0
                while i < 3:
                    label = send_iget_response(conn, success=True)
                    labellist[i] = label
                    i += 1
            
            elif cmd == "ILBL":
                try:
                    label_res = label_check(args, labellist)
                    false_count = 0
                    i = 0
                    while i < 3:
                        if label_res[i] == False:
                            false_count += 1
                        i += 1
                    if false_count != 0:
                        raise WrongLabelingException
                    # break
                except WrongLabelingException:
                    send_response(conn, success=False, info=str(false_count) + " of the label(s) is/are wrong.")
               
                except NoSuchLabelException:
                    send_response(conn, success=False, info= "No such label(s) found.")
                
                else:
                    send_response(conn, success=True)
            
            elif cmd == "EXIT":
                send_response(conn, success=True)
                break
            
            elif cmd in ["USER", "PASS"]:
                send_response(conn, success=False, info=cmd + " command is already sent and processed.")
                shutdown()
                
            else:
                send_response(conn, success=False, info="Unknown command.")
                shutdown()
        
    except ServerShutdownException:
        pass
    
    except ConnectionResetError as e:
        print(e)
        
    finally:
        conn.close()
        s.close()
    
