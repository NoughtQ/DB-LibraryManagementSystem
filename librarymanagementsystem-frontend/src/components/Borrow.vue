<template>
    <el-scrollbar height="100%" style="width: 100%;">

        <!-- 标题和操作按钮 -->
        <div style="margin-top: 20px; margin-left: 40px; margin-right: 32px; font-size: 2em; font-weight: bold;">
            借还书管理
            <el-button type="success" @click="openReturnBookDialog" style="float: right; margin-right: 10px;">
                <el-icon>
                    <Check />
                </el-icon>
                归还图书
            </el-button>
            <el-button type="primary" @click="openBorrowBookDialog" style="float: right; margin-right: 10px;">
                <el-icon>
                    <Plus />
                </el-icon>
                借阅图书
            </el-button>
        </div>

        <!-- 查询框 -->
        <div style="width:30%; margin:20px auto;">
            <el-input v-model="toQuery" style="display:inline; width: 200px;" placeholder="输入借书证ID"></el-input>
            <el-button style="margin-left: 10px;" type="primary" @click="queryBorrows">查询</el-button>
            <el-button style="margin-left: 10px;" @click="showAllBorrows">显示全部</el-button>
        </div>

        <!-- 借阅记录表格 -->
        <div style="margin: 20px 40px; margin-left: 11%">
            <el-table :data="tableData" style="width: 90%" border stripe
                :default-sort="{ prop: 'borrowTime', order: 'descending' }">
                <el-table-column prop="cardId" label="借书证ID" min-width="120" sortable />
                <el-table-column prop="bookId" label="图书ID" min-width="120" sortable />
                <el-table-column label="借出时间" min-width="200" sortable>
                    <template #default="scope">
                        {{ formatDate(scope.row.borrowTime) }}
                    </template>
                </el-table-column>
                <el-table-column label="归还时间" min-width="200" sortable>
                    <template #default="scope">
                        {{ scope.row.returnTime ? formatDate(scope.row.returnTime) : '未归还' }}
                    </template>
                </el-table-column>
            </el-table>
        </div>

        <!-- 借书对话框 -->
        <el-dialog v-model="borrowBookVisible" title="借阅图书" width="30%" align-center>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px;">
                借书证ID：
                <el-input v-model="borrowInfo.cardId" type="number" style="width: 15vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px;">
                图书ID：
                <el-input v-model="borrowInfo.bookId" type="number" style="margin-left: 16px; width: 15vw;" clearable />
            </div>

            <template #footer>
                <span>
                    <el-button @click="borrowBookVisible = false">取消</el-button>
                    <el-button type="primary" @click="confirmBorrowBook"
                        :disabled="!isBorrowFormValid">确定</el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 还书对话框 -->
        <el-dialog v-model="returnBookVisible" title="归还图书" width="30%" align-center>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px;">
                借书证ID：
                <el-input v-model="returnInfo.cardId" type="number" style="width: 15vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px;">
                图书ID：
                <el-input v-model="returnInfo.bookId" type="number" style="margin-left: 16px; width: 15vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px;">
                借书时间：
                <el-input v-model="returnInfo.borrowTimeStr" placeholder="格式: 2025/04/03 15:21:04.123" style="width: 15vw;" clearable />
            </div>

            <template #footer>
                <span>
                    <el-button @click="returnBookVisible = false">取消</el-button>
                    <el-button type="primary" @click="confirmReturnBook"
                        :disabled="!isReturnFormValid">确定</el-button>
                </span>
            </template>
        </el-dialog>

    </el-scrollbar>
</template>

<script>
import axios from 'axios';
import { Plus, Check } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

export default {
    data() {
        return {
            tableData: [],                     // 所有借阅记录
            toQuery: '',                       // 待查询内容(对某一借书证号进行查询)
            borrowBookVisible: false,          // 借书对话框可见性
            returnBookVisible: false,          // 还书对话框可见性
            borrowInfo: {                      // 借书信息
                cardId: '',
                bookId: ''
            },
            returnInfo: {                      // 还书信息
                cardId: '',
                bookId: '',
                borrowTimeStr: ''              // 借书时间字符串
            },
            currentTime: Date.now(),           // 当前时间，用于实时显示
            timerInterval: null,               // 定时器
            Plus,
            Check
        }
    },
    computed: {
        isBorrowFormValid() {
            return this.borrowInfo.cardId && this.borrowInfo.bookId;
        },
        isReturnFormValid() {
            return this.returnInfo.cardId && this.returnInfo.bookId;
        }
    },
    methods: {
        // 时间戳 -> 标准日期格式字符串
        formatDate(timestamp) {
            if (!timestamp) return '';
            const date = new Date(timestamp);
            
            const year = date.getFullYear();
            const month = String(date.getMonth() + 1).padStart(2, '0');
            const day = String(date.getDate()).padStart(2, '0');
            const hours = String(date.getHours()).padStart(2, '0');
            const minutes = String(date.getMinutes()).padStart(2, '0');
            const seconds = String(date.getSeconds()).padStart(2, '0');
            const milliseconds = String(date.getMilliseconds()).padStart(3, '0');
            
            return `${year}/${month}/${day} ${hours}:${minutes}:${seconds}.${milliseconds}`;
        },
        
        // 借阅记录查询
        async queryBorrows() {
            if (!this.toQuery) {
                ElMessage.warning('请输入借书证ID');
                return;
            }
            try {
                const response = await axios.get('/borrow', { params: { cardId: this.toQuery } });
                this.tableData = response.data;
            } catch (error) {
                console.error('查询借阅记录失败:', error);
                ElMessage.error('查询借阅记录失败');
            }
        },
     
        // 显示全部借阅记录
        async showAllBorrows() {
            try {
                const response = await axios.get('/borrow/all');
                this.tableData = response.data;
            } catch (error) {
                console.error('获取所有借阅记录失败:', error);
                ElMessage.error('获取所有借阅记录失败');
            }
        },
        
        // 借书对话框
        openBorrowBookDialog() {
            this.borrowInfo = {
                cardId: '',
                bookId: ''
            };
            this.borrowBookVisible = true;
            
            this.currentTime = Date.now();
            this.timerInterval = setInterval(() => {
                this.currentTime = Date.now();
            }, 1000);
        },
        
        // 借书处理函数
        async confirmBorrowBook() {
            try {
                if (this.timerInterval) {
                    clearInterval(this.timerInterval);
                    this.timerInterval = null;
                }
                
                const currentTime = Date.now();
                
                const borrowData = {
                    cardId: Number(this.borrowInfo.cardId),
                    bookId: Number(this.borrowInfo.bookId),
                    borrowTime: currentTime
                };
                
                console.log('Borrow time:', currentTime);
                const response = await axios.post('/borrow', borrowData);
                ElMessage.success('借书成功');
                this.borrowBookVisible = false;
                this.showAllBorrows();
            } catch (error) {
                let errorMsg = '借书失败，原因：';
                if (error.response && error.response.data) {
                    errorMsg += error.response.data;
                }
                this.borrowBookVisible = false;
                ElMessage.error(errorMsg);
            }
        },
        
        // 还书对话框
        openReturnBookDialog() {
            this.returnInfo = {
                cardId: '',
                bookId: '',
                borrowTimeStr: ''
            };
            this.returnBookVisible = true;
        },
        
        // 标准格式日期字符串 -> 时间戳
        parseDate(dateStr) {
            if (!dateStr) return null;
            try {
                // 处理格式形如 "2025/04/03 15:21:04.123"
                const parts = dateStr.split(' ');
                if (parts.length !== 2) return null;
                
                const dateParts = parts[0].split('/');
                if (dateParts.length !== 3) return null;
                
                let timeParts, milliseconds = 0;
                if (parts[1].includes('.')) {
                    const timeAndMs = parts[1].split('.');
                    timeParts = timeAndMs[0].split(':');
                    milliseconds = parseInt(timeAndMs[1]) || 0;
                } else {
                    timeParts = parts[1].split(':');
                }
                
                if (timeParts.length !== 3) return null;
                
                const year = parseInt(dateParts[0]);
                const month = parseInt(dateParts[1]) - 1;
                const day = parseInt(dateParts[2]);
                
                const hour = parseInt(timeParts[0]);
                const minute = parseInt(timeParts[1]);
                const second = parseInt(timeParts[2]);
                
                const date = new Date(year, month, day, hour, minute, second, milliseconds);
                return date.getTime();
            } catch (error) {
                console.error('日期解析错误:', error);
                return null;
            }
        },
        
        // 还书处理函数
        async confirmReturnBook() {
            try {
                const returnData = {
                    cardId: Number(this.returnInfo.cardId),
                    bookId: Number(this.returnInfo.bookId),
                    returnTime: Date.now()
                };
                
                if (this.returnInfo.borrowTimeStr) {
                    const borrowTime = this.parseDate(this.returnInfo.borrowTimeStr);
                    if (borrowTime) {
                        returnData.borrowTime = borrowTime;
                    } else {
                        ElMessage.warning('借书时间格式不正确，请使用格式: 2025/04/03 15:21:04.123');
                        return;
                    }
                } else {
                    ElMessage.warning('请提供借书时间');
                    return;
                }
                
                const response = await axios.put('/borrow', returnData);
                ElMessage.success('还书成功');
                this.returnBookVisible = false;
                this.showAllBorrows();
            } catch (error) {
                let errorMsg = '还书失败，原因：';
                if (error.response && error.response.data) {
                    errorMsg += error.response.data;
                }
                ElMessage.error(errorMsg);
            }
        }
    },
    mounted() {
        this.showAllBorrows();
    },
    beforeUnmount() {
        if (this.timerInterval) {
            clearInterval(this.timerInterval);
            this.timerInterval = null;
        }
    }
}
</script>

<style scoped>
.el-button {
    margin-left: 5px;
}
</style>