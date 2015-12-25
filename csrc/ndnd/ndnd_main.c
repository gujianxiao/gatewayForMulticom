/**
 * @file ndnd_main.c
 *
 * A NDNx program.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 *
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2009-2011, 2013 Palo Alto Research Center, Inc.
 *
 * This work is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2 as published by the
 * Free Software Foundation.
 * This work is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details. You should have received a copy of the GNU General Public
 * License along with this program; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA 02110-1301, USA.
 */

#include <signal.h>
#include <stddef.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <openssl/evp.h>
#include <openssl/err.h>

#include "ndnd_private.h"

#include <stddef.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <fcntl.h>
#include <openssl/evp.h>
#include <openssl/err.h>
#include <openssl/crypto.h>

//#include "ccnd_private.h"
#include <termios.h>
#include <netinet/ether.h>
#include <netpacket/packet.h>
#include <string.h>
#include <pthread.h>
#include <sys/time.h>

//modify by cb
#include <netinet/in.h>
#include <sys/socket.h>
int fdusb=0;
#include "ndnpoke.h"
#include "define.h"
//#include "ndw.h"

#define LISTENQ         1024    /* 2nd argument to listen() */
#define SA      struct sockaddr
#define SERV_PORT 11111
#define MAXLINE 1024*4

//remove the declar of socket 


//end modify here
int thread_flag=1;
pthread_t pid_listen;
pthread_t pid_topo;

pthread_t pid_work;
Msg *recv_data;
int data_flag = 0;
char *scope_name=NULL;

sem_t sem_queue;
sem_t sem_interest;
BTNode *topo_head=NULL;
/*modified by zhy on 20141230*/
uint8_t topo_tree_node_check[256];
/**
 * Set the USB Port
 */
int set_opt(int fd,int nSpeed, int nBits, char nEvent, int nStop)
{
    struct termios newtio,oldtio;
    if  ( tcgetattr( fd,&oldtio)  !=  0) {
        perror("SetupSerial 1");
        return -1;
    }
    bzero( &newtio, sizeof( newtio ) );
    newtio.c_cflag  |=  CLOCAL | CREAD;
    newtio.c_cflag &= ~CSIZE;

    switch( nBits )
    {
    case 7:
        newtio.c_cflag |= CS7;
        break;
    case 8:
        newtio.c_cflag |= CS8;
        break;
    }

    switch( nEvent )
    {
    case 'O':
        newtio.c_cflag |= PARENB;
        newtio.c_cflag |= PARODD;
        newtio.c_iflag |= (INPCK | ISTRIP);
        break;
    case 'E':
        newtio.c_iflag |= (INPCK | ISTRIP);
        newtio.c_cflag |= PARENB;
        newtio.c_cflag &= ~PARODD;
        break;
    case 'N':
        newtio.c_cflag &= ~PARENB;
        break;
    }

    switch( nSpeed )
    {
    case 2400:
        cfsetispeed(&newtio, B2400);
        cfsetospeed(&newtio, B2400);
        break;
    case 4800:
        cfsetispeed(&newtio, B4800);
        cfsetospeed(&newtio, B4800);
        break;
    case 9600:
        cfsetispeed(&newtio, B9600);
        cfsetospeed(&newtio, B9600);
        break;
    case 115200:
        cfsetispeed(&newtio, B115200);
        cfsetospeed(&newtio, B115200);
        break;
    case 460800:
        cfsetispeed(&newtio, B460800);
        cfsetospeed(&newtio, B460800);
        break;
    default:
        cfsetispeed(&newtio, B9600);
        cfsetospeed(&newtio, B9600);
        break;
    }
    if( nStop == 1 )
        newtio.c_cflag &=  ~CSTOPB;
    else if ( nStop == 2 )
    newtio.c_cflag |=  CSTOPB;
    newtio.c_cc[VTIME]  = 0;//重要
    newtio.c_cc[VMIN] = 1;//返回的最小值  重要
    tcflush(fd,TCIFLUSH);
    if((tcsetattr(fd,TCSANOW,&newtio))!=0)
    {
        perror("com set error");
        return -1;
    }
//  //printf("set done!\n\r");
    return 0;
}

static int
stdiologger(void *loggerdata, const char *format, va_list ap)
{
    FILE *fp = (FILE *)loggerdata;
    return(vfprintf(fp, format, ap));
}

void dfs_topo_tree(BTNode* node)
{
        if(node->child_num==0)
        {
                free(node);
                return ;
        }
        else
        {
                int i;
                for(i=0; i<node->child_num; i++)
                {
                        dfs_topo_tree(node->ptr[i]);
                }
                free(node);
        }
}

void topo_tree_recycling()
{
        if(topo_head->child_num == 0)
                return;
        dfs_topo_tree(topo_head->ptr[0]);
        topo_head->child_num = 0;
        topo_head->ptr[0] = NULL;
        return ;
}

void topo_tree_build(BTNode** node, uint16_t *node_list, int index, uint8_t *topo_rebuild_flag)
{
        if(index < 0)
                return ;
        int i;
        for(i=0; i<(*node)->child_num; i++)
        {
                if((*node)->ptr[i]->nodeID == node_list[index])
                {
                        topo_tree_build(&((*node)->ptr[i]), node_list, index-1, topo_rebuild_flag);
                        break;
                }
        }
        if(i==(*node)->child_num)
        {
                if(topo_tree_node_check[node_list[index]] == 1)
                {
                        *topo_rebuild_flag = 1;
                            return ;
                }
                (*node)->ptr[i] = malloc(sizeof(BTNode));
                (*node)->ptr[i]->nodeID = node_list[index];
                (*node)->ptr[i]->parent = (*node);
                (*node)->ptr[i]->child_num = 0;
                (*node)->child_num++;
                topo_tree_node_check[node_list[index]] = 1;
                topo_tree_build(&((*node)->ptr[i]), node_list, index-1, topo_rebuild_flag);
        }
}

void topo_tree_level(BTNode **bt_buf, int n)
{
    if(n==0)
        return ;
    int i, j, k=0;
    BTNode *tmp_buf[PTR_MAX];
    for(i=0; i<n; i++)
    {
        printf("%d ", bt_buf[i]->nodeID);
        for(j=0; j<bt_buf[i]->child_num; j++)
            tmp_buf[k++]=bt_buf[i]->ptr[j];
    }
    printf("\n");
    topo_tree_level(tmp_buf, k);
}

void topo_tree_show(BTNode* head)
{
    printf("show the topo tree!~\n");
    if(head==NULL)
    {
        printf("the head is null!~\n");
        return;
    }
    BTNode *bt_buf[PTR_MAX];
    bt_buf[0]=head;
    topo_tree_level(bt_buf, 1);
}

void topo_management(void* arg)
{
    DEBUG printf("create topo management thread successed!!\n");
    uint8_t topo_rebuild_flag=0;
    while(thread_flag)
    {
            sem_wait(&sem_queue);
            topo_msg* topo = queue[bottom++];
            int i;
            if(bottom>=PTR_MAX)
                    bottom = bottom%PTR_MAX;
            for(i=0; i<topo->num; i++)
            {
                printf("%d -> ", topo->data[i]);
            }
            printf("NULL\n");
            //topo_tree_recycling();
            topo_tree_build(&topo_head, topo->data,topo->num-1, &topo_rebuild_flag);
            //
            printf("debug topo_managment1:end of topo_tree_build\n");
            if(topo_rebuild_flag)
            {
                        memset(topo_tree_node_check, 0, 256);
                        topo_tree_recycling();
                        printf("debug management2:end of topo_tree_recycling\n");
                        topo_rebuild_flag=0;
            }
            topo_tree_show(topo_head);
            printf("debug management3:end of topo_tree_show\n");
            free(topo);
    }
    return ;
}

void usbportListen(void* arg)
{

    DEBUG printf("create listening thread successed!!\n");
    int i,j;
    int nread;
    //unsigned char start_mark[] = {0x7e,0x45,0x00};
    char *start_p;
    char usbbuf[1024];
    unsigned char name_buf[64];
    unsigned char content_buf[10];
    uint16_t content;
    int total_read=0;
    int datagotflag=0;

    int move_pos;
    recv_data = (Msg *)malloc(sizeof(Msg));
    total_read=0;


	//modify by cb




    // build udp connect between Yulin and Beijing
    char mesg[MAXLINE];
    char recv_mesg[MAXLINE];
    char back_mesg[]="From Beijing :connection build success!\n";
    int n=0;
    len=sizeof(Yulin_cliaddr);
    while(n <= 0 ){
    	printf("waiting..........\n");
    	n = recvfrom(Beijing_sockfd, recv_mesg, MAXLINE, 0, (SA *) &Yulin_cliaddr,&len);
	printf("receive state:%d  \n", n);
    	//perror("didn't get the connect info from Yulin!");
    }
    printf("receive from client:%d   %s\n", n,  recv_mesg);


    n =  sendto(Beijing_sockfd, back_mesg, sizeof(back_mesg), 0, (SA *) &Yulin_cliaddr, len);
    printf("send back to Yulin:%s\n",back_mesg);
    printf("send state:%d  \n", n);
    if ( n == -1){
	perror("");
	exit(0);
    }
    

    //end modify


    while(thread_flag)
    {
        //memset(usbbuf, 0, 1024);
	#include <arpa/inet.h>

	char *strtest;
	long xxxx;

        datagotflag=0;
        while(total_read<SHORTEST_DATA_FRAME_LEN)//每次读取至少DATA_FRAME_LEN字节的数据(数据包长DATA_FRAME_LEN)
        {
        	//modify by cb
        	   nread= recvfrom(Beijing_sockfd, usbbuf+total_read, MAXLINE, 0, (SA *) &Yulin_cliaddr, &len);//读udp数据
                   printf("receive state:%d,receive len:%d\n",nread,len);
//xxxx = (long)Yulin_cliaddr.sin_addr.s_addr;
/*
		strtest = inet_ntoa(Yulin_cliaddr.sin_addr);
		if(!strtest)
                	printf("get ip failed!\n");
                else
			printf("ip = %s\n", strtest);
  //*/
        	   printf("receive socket port:%d,socket IP:%s\n",(int)Yulin_cliaddr.sin_port,inet_ntoa(Yulin_cliaddr.sin_addr));
               n =  sendto(Beijing_sockfd, back_mesg, sizeof(back_mesg), 0, (SA *) &Yulin_cliaddr, len);//测试持续udp通信
               printf("get enough data then send flag back to Yulin:%s\n",back_mesg);
               printf("send state:%d  \n", n);
               if ( n == -1){
	          perror("");
	          exit(0);
               }
//           nread = read(fdusb, usbbuf+total_read, 1024); //读USB串口
               //end modify
             total_read += nread;
        }
         DEBUG printhex_macaddr(usbbuf, total_read, " ");
         printf("\n");
        for(i=0; i<total_read-7; i++)//找到数据包标志
        {
            if(usbbuf[i] == 0x7e && usbbuf[i+1] == 0x45 && usbbuf[i+2] == 0x00 && usbbuf[i+3] == 0x00
                && usbbuf[i+4] == 0x00 && usbbuf[i+5] == 0x00 && usbbuf[i+6] == 0x01)
            {
                datagotflag=1;
                //DEBUG printf("total_read=%d, i=%d\n", total_read, i);
                while(total_read-i<SHORTEST_DATA_FRAME_LEN)//标志后的数据不够DATA_FRAME_LEN字节，继续读数据
                {
                    nread= recvfrom(Beijing_sockfd, mesg+total_read, MAXLINE, 0, (SA *) &Yulin_cliaddr, &len);
                    total_read += nread;
                    //DEBUG printf("total_read=%d, i=%d\n", total_read, i);
                }
                start_p = usbbuf + i;//start_p point to the begining of the frame
                break;
            }
        }
        if(!datagotflag)//没找到标志位，不予处理
        {
                printf("not found the start flag!\n");

                for(move_pos=total_read-1; move_pos>=total_read-7; move_pos--)
                {
                    if(usbbuf[move_pos] == 0x7e)
                        break;
                }
                if(move_pos<total_read-7)
                {
                    total_read = 0;
                    continue;
                }
                for(j=move_pos,i=0; j<total_read; j++, i++)
                {
                        usbbuf[i] = usbbuf[j];
                }
                total_read -= move_pos;
                continue;
        }
        //DEBUG printf("got the packet and nread=%d\n", total_read);

       if (*(start_p+10) == DATA)//内容包
        {
                    while(total_read<28+start_p-usbbuf)//每次读取至少28字节的数据
                    {
                        nread= recvfrom(Beijing_sockfd, mesg+total_read, MAXLINE, 0, (SA *) &Yulin_cliaddr, &len);//读UDP串口
                        total_read += nread;
                    }
                    //DEBUG printf("Got a data message!\n");
                    DEBUG printhex_macaddr(start_p, 28, " ");
                    DEBUG printf("\n");

                    memcpy(recv_data, start_p + 10, sizeof(Msg));
                    if(recv_data->msgType == DATA)
                    {
                        DEBUG printf("Got Sensor data!\n");

                        memset(name_buf, 0, sizeof(name_buf));
                        memset(content_buf, 0, sizeof(content_buf));
                        content = recv_data->data;
                        sprintf(name_buf, "ndn:/%s/ints/%hd,%hd/%hd,%hd/", NAME_PREFIX, recv_data->msgName.ability.leftUp.x, recv_data->msgName.ability.leftUp.y,
                                                                                                recv_data->msgName.ability.rightDown.x, recv_data->msgName.ability.rightDown.y);
                        if(recv_data->msgName.dataType == Light)
                            strcat(name_buf, "light");
                        if(recv_data->msgName.dataType == Temp)
                            strcat(name_buf, "temp");
                        if(recv_data->msgName.dataType == Humidity)
                            strcat(name_buf, "humidity");
                        printf("interest name = %s\n", name_buf);
                        sprintf(content_buf, "%hd\n", content);
                        printf("content data = %s", content_buf);
                        pack_data_content(name_buf, content_buf);

                        data_flag = 1;
                    }
        }
        else if (*(start_p+10) == TOPOLOGY)//topology packet
        {
                while(total_read<37+start_p-usbbuf)//每次读取至少38字节的数据
                {
                        nread= recvfrom(Beijing_sockfd, mesg+total_read, MAXLINE, 0, (SA *) &Yulin_cliaddr,  &len);//读UDP串口
                        total_read += nread;
                }
                DEBUG printf("Got a topology message!\n");
                DEBUG printhex_macaddr(start_p, 37, " ");
                DEBUG printf("\n");
                //modify by cb for debug topo data error
                int topo_data_len=0;
                int topo_error_flag=0;
                topo_msg *recv_topo = (topo_msg*) malloc(sizeof(topo_msg));
                memcpy(recv_topo, start_p + 12, sizeof(topo_msg));
                printf("topo num:%d\n", recv_topo->num);
                if(recv_topo->num>10){
                	printf("topo num error!\n");
                	topo_error_flag=1;
                }

                while(topo_data_len<10)
                {
                printf("topo data%d:%d\n",topo_data_len,recv_topo->data[topo_data_len]);

                if(recv_topo->data[topo_data_len]>100){
                	printf("topo data error! change it to 0!\n");
                	recv_topo->data[topo_data_len]=0;
                	topo_error_flag=1;
                }
                topo_data_len++;
                }
                printf("topo_error_flag:%d\n",topo_error_flag);
                if(topo_error_flag==0){
                      if(top!=(bottom-1+PTR_MAX)%PTR_MAX)
                      {
                           queue[top++] = recv_topo;
                           if(top>=PTR_MAX)
                                   top = top%PTR_MAX;
                          sem_post(&sem_queue);
                      }
                }
                else printf("abandon this topo message!");
                //end modify
        }
        else if(*(start_p+10) == MAPPING)
        {
                while(total_read<25+start_p-usbbuf)//每次读取至少26字节的数据
                {
                        nread= recvfrom(Beijing_sockfd, mesg+total_read, MAXLINE, 0, (SA *) &Yulin_cliaddr,  &len);//读UDP串口
                        total_read += nread;
                }
                DEBUG printf("Got a mapping message!\n");
                DEBUG printhex_macaddr(start_p, 25, " ");
                DEBUG printf("\n");
//                printf("debug1\n");
                node_info *node = (node_info*) malloc(sizeof(node_info));
//                printf("debug2\n");
                if(!node){
               perror("malloc failed!:"); 
               exit(0);
               }

                memcpy(node, start_p + 12, sizeof(node_info));
                //modify by cb for debug
//                printf("debug3\n");
                printf("nodeId:%d\n",node->nodeID);
                if(node->nodeID<256&&node->nodeID>0){
//                printf("node.x:%d\n",node->coordinate.leftUp.x);
//                printf("node.y:%d\n",node->coordinate.leftUp.y);
                nodeID_mapping_table[node->nodeID].coordinate.x=node->coordinate.leftUp.x;
//                printf("debug3.1\n");
                nodeID_mapping_table[node->nodeID].coordinate.y=node->coordinate.leftUp.y;
//                printf("debug4\n");
                nodeID_mapping_table[node->nodeID].expire = 5;
//                printf("debug5\n");
                printf("nodeID=%d->(%d,%d)\n", node->nodeID, nodeID_mapping_table[node->nodeID].coordinate.x, nodeID_mapping_table[node->nodeID].coordinate.y);

               }
               else printf("nodeID out of range!nodID:%d\n",node->nodeID);
               free(node);
               //end modify
        }
        for(move_pos=total_read-1; move_pos>=total_read-7; move_pos--)
        {
            if(usbbuf[move_pos] == 0x7e)
                break;
        }
        if(move_pos<total_read-7)
        {
            total_read = 0;
            continue;
        }
        for(j=move_pos,i=0; j<total_read; j++, i++)
        {
                usbbuf[i] = usbbuf[j];
        }
        total_read -= move_pos;
    }
    return ;
}


void refresh_node_mapping_table(int sig)//定时器控制
{
    printf("timer was fired!!\n");
    int i;
    for(i=0; i<10; i++)
    {
        if(nodeID_mapping_table[i].expire>0)
            --nodeID_mapping_table[i].expire;
    }
}

void worker_thread(void *arg)
{
    char name[64]={0};
    char content[1024]= {0};
    char temp[128]= {0};
    struct  timeval  start;
    struct  timeval  end;

    while(thread_flag)
    {
        sem_wait(&sem_interest);
        data_flag = 0;
        strcpy(name, scope_name);
        gettimeofday(&start,NULL);
        gettimeofday(&end,NULL);
        while(end.tv_sec-start.tv_sec <= 30)
        {
            if(data_flag == 1)
            {
                    sprintf(temp, "%d,%d %d\n", recv_data->msgName.ability.leftUp.x, recv_data->msgName.ability.leftUp.y, recv_data->data);
                    strcat(content, temp);
                    data_flag = 0;
            }
            gettimeofday(&end,NULL);
        }
        pack_data_content(name, content);
        memset(content, 0, sizeof(content));
    }
}

int gateway_init()//网关初始化操作
{
    int nret;
    // modify by cb
//    fdusb = open(USB_PATH_PORT, O_RDWR);//打开串口
//    if (fdusb == -1)
//    {
//        printf("open USB failed!!\n");
//        return 1;
//    }
//    nret = set_opt(fdusb,115200, 8, 'N', 1);//设置串口属性
//    if (nret == -1)
//        return 1;


    //bind the socket with Yulin
    Beijing_sockfd = socket(AF_INET, SOCK_DGRAM, 0);
    bzero(&Beijing_servaddr, sizeof(Beijing_servaddr));
    Beijing_servaddr.sin_family      = AF_INET;
    Beijing_servaddr.sin_addr.s_addr = htonl(INADDR_ANY);
    Beijing_servaddr.sin_port        = htons(SERV_PORT);
    bind(Beijing_sockfd, (SA *) &Beijing_servaddr, sizeof(Beijing_servaddr));

    //end modify

    int res=0;
    thread_flag=1;

    res = sem_init(&sem_queue, 0, 0);//初始化信号量
    if(res == -1)
    {
        printf("semaphore sem_queue initialization failed!\n");
        return 1;
    }
    res = sem_init(&sem_interest, 0, 0);//初始化信号量
    if(res == -1)
    {
        printf("semaphore sem_interest initialization failed!\n");
        return 1;
    }

    res = pthread_create(&pid_listen, NULL, usbportListen, NULL);//开启接收线程
    if(res!=0)
    {
        printf("create listending thread error!!\n");
        return 1;
    }
    topo_head = (BTNode*)malloc(sizeof(BTNode));
    memset(topo_head, 0, sizeof(BTNode));

    res = pthread_create(&pid_topo, NULL, topo_management, NULL);//开启拓扑管理线程
    if(res!=0)
    {
        printf("create topology thread error!!\n");
        return 1;
    }

    res = pthread_create(&pid_work, NULL, worker_thread, NULL);//开启拓扑管理线程
    if(res!=0)
    {
        printf("create work thread error!!\n");
        return 1;
    }

    memset(nodeID_mapping_table, 0, sizeof(node_mapping)*256);
    int i;
    for(i=0; i<256; i++)
    {
        nodeID_mapping_table[i].expire = -1;
    }

    memset(topo_tree_node_check, 0, 256);
    scope_name = (char*)malloc(sizeof(char)*64);
    struct itimerval t;//设置时间间隔
    t.it_interval.tv_sec=50;
    t.it_interval.tv_usec=0;
    t.it_value.tv_sec=50;
    t.it_value.tv_usec=0;

    if(setitimer(ITIMER_REAL, &t, NULL) < 0)//初始化定时器
    {
        printf("settimer error!\n");
        return 1;
    }
    signal(SIGALRM, refresh_node_mapping_table);//注册SIGALRM信号

    top = 0;
    bottom = 0;

    return 0;
}

int
main(int argc, char **argv)
{
    struct ndnd_handle *h;

    if (argc > 1) {
        fprintf(stderr, "%s", ndnd_usage_message);
        exit(1);
    }
    signal(SIGPIPE, SIG_IGN);
    h = ndnd_create(argv[0], stdiologger, stderr);
    if (h == NULL)
        exit(1);

    /*modified by zhy on 20131112*/
    if(gateway_init())
    {
        printf("gateway initial failed!\n");
        exit(1);
    }
    ndnd_run(h);
    thread_flag=0;//recycle the thread
    pthread_detach(pid_listen);
    pthread_detach(pid_topo);

    close(fdusb);
    ndnd_msg(h, "exiting.");
    ndnd_destroy(&h);
    ERR_remove_state(0);
    EVP_cleanup();
    CRYPTO_cleanup_all_ex_data();
    exit(0);
}

