import { useState } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Typography,
  IconButton,
  Chip,
  Alert,
  Snackbar,
} from '@mui/material';
import { DataGrid, GridColDef, GridRenderCellParams } from '@mui/x-data-grid';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Block as BlockIcon,
  CheckCircle as CheckCircleIcon,
} from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { userService, User, CreateUserRequest, UpdateUserRequest } from '../api/userService';

export default function Users() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [search, setSearch] = useState('');
  const [dialogOpen, setDialogOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' as 'success' | 'error' });

  const { register, handleSubmit, reset, formState: { errors } } = useForm<CreateUserRequest>();

  const { data: usersData, isLoading } = useQuery({
    queryKey: ['users', page, pageSize, search],
    queryFn: () => userService.getAll({ page, size: pageSize, search }),
  });

  const createMutation = useMutation({
    mutationFn: (data: CreateUserRequest) => userService.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
      setDialogOpen(false);
      reset();
      setSnackbar({ open: true, message: 'Utilisateur créé avec succès', severity: 'success' });
    },
    onError: () => {
      setSnackbar({ open: true, message: 'Erreur lors de la création', severity: 'error' });
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateUserRequest }) => userService.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
      setDialogOpen(false);
      setSelectedUser(null);
      reset();
      setSnackbar({ open: true, message: 'Utilisateur mis à jour', severity: 'success' });
    },
    onError: () => {
      setSnackbar({ open: true, message: 'Erreur lors de la mise à jour', severity: 'error' });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => userService.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
      setDeleteDialogOpen(false);
      setSelectedUser(null);
      setSnackbar({ open: true, message: 'Utilisateur supprimé', severity: 'success' });
    },
    onError: () => {
      setSnackbar({ open: true, message: 'Erreur lors de la suppression', severity: 'error' });
    },
  });

  const toggleActiveMutation = useMutation({
    mutationFn: (user: User) => user.active ? userService.deactivate(user.id) : userService.activate(user.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
      setSnackbar({ open: true, message: 'Statut modifié', severity: 'success' });
    },
  });

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'email', headerName: 'Email', width: 220 },
    { field: 'firstName', headerName: 'Prénom', width: 130 },
    { field: 'lastName', headerName: 'Nom', width: 130 },
    { field: 'phoneNumber', headerName: 'Téléphone', width: 130 },
    {
      field: 'active',
      headerName: 'Statut',
      width: 100,
      renderCell: (params: GridRenderCellParams) => (
        <Chip
          label={params.value ? 'Actif' : 'Inactif'}
          color={params.value ? 'success' : 'default'}
          size="small"
        />
      ),
    },
    {
      field: 'createdAt',
      headerName: 'Créé le',
      width: 120,
      valueFormatter: (value: string) => new Date(value).toLocaleDateString('fr-FR'),
    },
    {
      field: 'actions',
      headerName: 'Actions',
      width: 150,
      sortable: false,
      renderCell: (params: GridRenderCellParams<User>) => (
        <Box>
          <IconButton
            size="small"
            onClick={() => {
              setSelectedUser(params.row);
              setDialogOpen(true);
            }}
          >
            <EditIcon fontSize="small" />
          </IconButton>
          <IconButton
            size="small"
            onClick={() => toggleActiveMutation.mutate(params.row)}
          >
            {params.row.active ? <BlockIcon fontSize="small" /> : <CheckCircleIcon fontSize="small" />}
          </IconButton>
          <IconButton
            size="small"
            color="error"
            onClick={() => {
              setSelectedUser(params.row);
              setDeleteDialogOpen(true);
            }}
          >
            <DeleteIcon fontSize="small" />
          </IconButton>
        </Box>
      ),
    },
  ];

  const onSubmit = (data: CreateUserRequest) => {
    if (selectedUser) {
      updateMutation.mutate({ id: selectedUser.id, data });
    } else {
      createMutation.mutate(data);
    }
  };

  const handleOpenDialog = () => {
    setSelectedUser(null);
    reset({});
    setDialogOpen(true);
  };

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4" fontWeight="bold">
          Utilisateurs
        </Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={handleOpenDialog}>
          Nouvel utilisateur
        </Button>
      </Box>

      <Card>
        <CardContent>
          <TextField
            fullWidth
            placeholder="Rechercher par email, nom ou prénom..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            sx={{ mb: 2 }}
          />

          <DataGrid
            rows={usersData?.data || []}
            columns={columns}
            loading={isLoading}
            paginationMode="server"
            rowCount={usersData?.pageInfo?.totalElements || 0}
            pageSizeOptions={[5, 10, 25]}
            paginationModel={{ page, pageSize }}
            onPaginationModelChange={(model) => {
              setPage(model.page);
              setPageSize(model.pageSize);
            }}
            autoHeight
            disableRowSelectionOnClick
          />
        </CardContent>
      </Card>

      {/* Create/Edit Dialog */}
      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
        <form onSubmit={handleSubmit(onSubmit)}>
          <DialogTitle>
            {selectedUser ? 'Modifier l\'utilisateur' : 'Nouvel utilisateur'}
          </DialogTitle>
          <DialogContent>
            <TextField
              fullWidth
              label="Email"
              margin="normal"
              {...register('email', { required: 'Email requis' })}
              error={!!errors.email}
              helperText={errors.email?.message}
              defaultValue={selectedUser?.email}
            />
            {!selectedUser && (
              <TextField
                fullWidth
                label="Mot de passe"
                type="password"
                margin="normal"
                {...register('password', { required: !selectedUser && 'Mot de passe requis' })}
                error={!!errors.password}
                helperText={errors.password?.message}
              />
            )}
            <TextField
              fullWidth
              label="Prénom"
              margin="normal"
              {...register('firstName', { required: 'Prénom requis' })}
              error={!!errors.firstName}
              helperText={errors.firstName?.message}
              defaultValue={selectedUser?.firstName}
            />
            <TextField
              fullWidth
              label="Nom"
              margin="normal"
              {...register('lastName', { required: 'Nom requis' })}
              error={!!errors.lastName}
              helperText={errors.lastName?.message}
              defaultValue={selectedUser?.lastName}
            />
            <TextField
              fullWidth
              label="Téléphone"
              margin="normal"
              {...register('phoneNumber')}
              defaultValue={selectedUser?.phoneNumber}
            />
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setDialogOpen(false)}>Annuler</Button>
            <Button type="submit" variant="contained">
              {selectedUser ? 'Modifier' : 'Créer'}
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      {/* Delete Confirmation Dialog */}
      <Dialog open={deleteDialogOpen} onClose={() => setDeleteDialogOpen(false)}>
        <DialogTitle>Confirmer la suppression</DialogTitle>
        <DialogContent>
          <Typography>
            Êtes-vous sûr de vouloir supprimer l'utilisateur {selectedUser?.firstName} {selectedUser?.lastName} ?
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteDialogOpen(false)}>Annuler</Button>
          <Button
            color="error"
            variant="contained"
            onClick={() => selectedUser && deleteMutation.mutate(selectedUser.id)}
          >
            Supprimer
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar
        open={snackbar.open}
        autoHideDuration={3000}
        onClose={() => setSnackbar({ ...snackbar, open: false })}
      >
        <Alert severity={snackbar.severity}>{snackbar.message}</Alert>
      </Snackbar>
    </Box>
  );
}
